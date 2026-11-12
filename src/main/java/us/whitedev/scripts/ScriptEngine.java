package us.whitedev.scripts;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import us.whitedev.helpers.PacketHelper;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ScriptEngine {
    private final ScriptRuntime.Parser parser;
    private final BooleanSupplier isRunning;
    private final Runnable requestStop;
    private final Consumer<String> log;

    private final Map<String, VarEntry> vars = new HashMap<>();
    private final Map<String, ScriptRuntime.FuncDeclStmt> funcs = new HashMap<>();
    private final ArrayDeque<Map<String, VarEntry>> scopes = new ArrayDeque<>();
    private boolean started = false;
    private int loopDepth = 0;
    private int funcDepth = 0;
    private final ArrayDeque<String> callStack = new ArrayDeque<>();

    private static final class BreakSignal extends RuntimeException {
    }

    private static final class ContinueSignal extends RuntimeException {
    }

    private static final class ReturnSignal extends RuntimeException {
        final Value value;

        ReturnSignal(Value value) {
            this.value = value;
        }
    }

    ScriptEngine(String src, BooleanSupplier isRunning, Runnable requestStop, Consumer<String> log) {
        this.parser = new ScriptRuntime.Parser(new ScriptRuntime.Lexer(src == null ? "" : src));
        this.isRunning = isRunning == null ? () -> true : isRunning;
        this.requestStop = requestStop == null ? () -> {
        } : requestStop;
        this.log = log;
        this.scopes.push(vars);
    }

    void run() {
        while (isRunning.getAsBoolean() && !parser.isAtEnd()) {
            ScriptRuntime.Stmt s = parser.parseStatement();
            if (s == null) break;
            evalStatement(s);
            parser.consumeOptional(ScriptRuntime.TokenKind.SEMI);
        }
    }

    private void evalStatement(ScriptRuntime.Stmt s) {
        if (!isRunning.getAsBoolean() || s == null) return;

        if (s instanceof ScriptRuntime.FuncDeclStmt fs) {
            if (funcs.containsKey(fs.name)) {
                throw new ScriptRuntime.ScriptError(fs.pos, "Function already defined: " + fs.name);
            }
            funcs.put(fs.name, fs);
            return;
        }

        if (s instanceof ScriptRuntime.StartStmt) {
            started = true;
            return;
        }

        if (s instanceof ScriptRuntime.StopStmt) {
            requestStop.run();
            return;
        }

        if (!started) return;

        if (s instanceof ScriptRuntime.ReturnStmt rs) {
            if (funcDepth <= 0) {
                throw new ScriptRuntime.ScriptError(rs.pos, "return outside function");
            }
            Value v = rs.expr == null ? Value.nullValue() : evalExpr(rs.expr);
            throw new ReturnSignal(v);
        }

        if (s instanceof ScriptRuntime.TryStmt ts) {
            try {
                if (ts.tryBody != null) {
                    for (ScriptRuntime.Stmt st : ts.tryBody) {
                        if (!isRunning.getAsBoolean()) break;
                        evalStatement(st);
                    }
                }
            } catch (ScriptRuntime.ScriptError e) {
                Map<String, VarEntry> catchScope = new HashMap<>();
                scopes.push(catchScope);
                try {
                    catchScope.put(ts.catchName, new VarEntry(Value.string(e.getMessage()), true));
                    if (ts.catchBody != null) {
                        for (ScriptRuntime.Stmt st : ts.catchBody) {
                            if (!isRunning.getAsBoolean()) break;
                            evalStatement(st);
                        }
                    }
                } finally {
                    scopes.pop();
                }
            }
            return;
        }

        if (s instanceof ScriptRuntime.LoopStmt ls) {
            Value cntV = evalExpr(ls.count);
            int cnt = Math.max(0, cntV.asInt(ls.pos));

            Map<String, VarEntry> cur = currentScope();
            VarEntry prev = cur.get(ls.varName);
            boolean hadPrev = cur.containsKey(ls.varName);
            try {
                loopDepth++;

                boolean breakLoop = false;
                for (int i = 0; i < cnt && isRunning.getAsBoolean(); i++) {
                    cur.put(ls.varName, new VarEntry(Value.number(i), true));
                    boolean continueLoop = false;
                    if (ls.body != null) {
                        for (ScriptRuntime.Stmt bs : ls.body) {
                            if (!isRunning.getAsBoolean()) break;
                            try {
                                evalStatement(bs);
                            } catch (ContinueSignal ignored) {
                                continueLoop = true;
                                break;
                            } catch (BreakSignal ignored) {
                                breakLoop = true;
                                break;
                            }
                        }
                    }
                    if (breakLoop) break;
                    if (continueLoop) continue;
                }
            } finally {
                loopDepth = Math.max(0, loopDepth - 1);
                if (hadPrev) cur.put(ls.varName, prev);
                else cur.remove(ls.varName);
            }
            return;
        }

        if (s instanceof ScriptRuntime.BlockStmt bs) {
            if (bs.body != null) {
                for (ScriptRuntime.Stmt st : bs.body) {
                    if (!isRunning.getAsBoolean()) break;
                    evalStatement(st);
                }
            }
            return;
        }

        if (s instanceof ScriptRuntime.IfStmt is) {
            Value cond = evalExpr(is.condition);
            if (isTruthy(cond)) {
                if (is.thenBody != null) {
                    for (ScriptRuntime.Stmt st : is.thenBody) {
                        if (!isRunning.getAsBoolean()) break;
                        evalStatement(st);
                    }
                }
            } else if (is.elseBranch != null) {
                evalStatement(is.elseBranch);
            }
            return;
        }

        if (s instanceof ScriptRuntime.BreakStmt) {
            if (loopDepth <= 0) {
                throw new ScriptRuntime.ScriptError(s.pos, "break outside loop");
            }
            throw new BreakSignal();
        }

        if (s instanceof ScriptRuntime.ContinueStmt) {
            if (loopDepth <= 0) {
                throw new ScriptRuntime.ScriptError(s.pos, "continue outside loop");
            }
            throw new ContinueSignal();
        }

        if (s instanceof ScriptRuntime.LetStmt ls) {
            Value v = evalExpr(ls.expr);
            Map<String, VarEntry> cur = currentScope();
            if (cur.containsKey(ls.name)) {
                throw new ScriptRuntime.ScriptError(ls.pos, "Variable already defined: " + ls.name);
            }
            cur.put(ls.name, new VarEntry(v, ls.isConst));
            return;
        }

        if (s instanceof ScriptRuntime.AssignStmt as) {
            Value v = evalExpr(as.expr);
            assignVar(as.name, v, as.pos);
            return;
        }

        if (s instanceof ScriptRuntime.ExprStmt es) {
            evalExpr(es.expr);
        }
    }

    private Value evalExpr(ScriptRuntime.Expr e) {
        if (!isRunning.getAsBoolean()) return Value.nullValue();
        if (e instanceof ScriptRuntime.LiteralExpr le) return le.value;

        if (e instanceof ScriptRuntime.ListLiteralExpr lle) {
            ListTag lt = new ListTag();
            if (lle.items != null) {
                for (ScriptRuntime.Expr it : lle.items) {
                    lt.add(toTag(evalExpr(it), lle.pos));
                }
            }
            return Value.tag(lt);
        }

        if (e instanceof ScriptRuntime.MapLiteralExpr mle) {
            if (mle.entries == null || mle.entries.isEmpty()) {
                return Value.tag(new CompoundTag());
            }

            boolean allIntKeys = true;
            boolean allItemValues = true;
            List<Integer> slots = new ArrayList<>();
            List<ItemStack> items = new ArrayList<>();
            for (ScriptRuntime.MapEntry en : mle.entries) {
                Value kV = evalExpr(en.key());
                Value vV = evalExpr(en.value());

                try {
                    slots.add(kV.asInt(mle.pos));
                } catch (ScriptRuntime.ScriptError ex) {
                    allIntKeys = false;
                }

                if (vV.kind != ValueKind.ITEM || vV.item == null) {
                    allItemValues = false;
                } else {
                    items.add(vV.item);
                }
            }

            if (allIntKeys && allItemValues && slots.size() == items.size()) {
                Int2ObjectOpenHashMap<ItemStack> map = new Int2ObjectOpenHashMap<>();
                for (int i = 0; i < slots.size(); i++) {
                    map.put(slots.get(i), items.get(i).copy());
                }
                return Value.itemMap(map);
            }

            CompoundTag ct = new CompoundTag();
            for (ScriptRuntime.MapEntry en : mle.entries) {
                String k = evalExpr(en.key()).asString();
                Tag v = toTag(evalExpr(en.value()), mle.pos);
                ct.put(k, v);
            }
            return Value.tag(ct);
        }

        if (e instanceof ScriptRuntime.VarExpr ve) {
            VarEntry v = findVar(ve.name);
            if (v != null) return v.value;
            ScriptRuntime.FuncDeclStmt fn = funcs.get(ve.name);
            if (fn != null) {
                return Value.function((args, p) -> invokeUserFunction(fn, args, p));
            }
            if ("create".equals(ve.name)) return Value.createNamespace();
            if ("debug".equals(ve.name)) return Value.debugNamespace();
            if ("nbt".equals(ve.name)) return Value.nbtNamespace();
            if ("env".equals(ve.name)) return Value.envNamespace();
            if ("ClickType".equals(ve.name)) return Value.clickTypeNamespace();
            if ("packet".equals(ve.name)) return Value.packetNamespace();
            if ("sleep".equals(ve.name)) {
                return Value.function((args, p) -> {
                    if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "sleep(ms) expects 1 arg");
                    int ms = Math.max(0, Math.min(60_000, args.get(0).asInt(p)));
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    return Value.nullValue();
                });
            }
            if ("yield".equals(ve.name)) {
                return Value.function((args, p) -> {
                    if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(p, "yield() expects 0 args");
                    Thread.yield();
                    return Value.nullValue();
                });
            }
            if ("assert".equals(ve.name)) {
                return Value.function((args, p) -> {
                    if (args.isEmpty() || args.size() > 2)
                        throw new ScriptRuntime.ScriptError(p, "assert(cond[,message])");
                    if (!isTruthy(args.get(0))) {
                        String msg = args.size() == 2 ? args.get(1).asString() : "Assertion failed";
                        throw new ScriptRuntime.ScriptError(p, msg);
                    }
                    return Value.nullValue();
                });
            }
            if ("send".equals(ve.name)) {
                return Value.function((args, p) -> {
                    if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "send(packet) expects 1 arg");
                    Value v0 = args.get(0);
                    if (v0.kind != ValueKind.PACKET || v0.packet == null) {
                        throw new ScriptRuntime.ScriptError(p, "send expects a packet");
                    }
                    ServerboundContainerClickPacket pkt = v0.packet;
                    Minecraft.getInstance().execute(() -> PacketHelper.send(pkt));
                    return Value.nullValue();
                });
            }
            if (isClickTypeName(ve.name)) return Value.string(ve.name);
            throw new ScriptRuntime.ScriptError(ve.pos, "Unknown identifier: " + ve.name);
        }

        if (e instanceof ScriptRuntime.BinaryExpr be) {
            if (be.op == ScriptRuntime.TokenKind.OR_OR) {
                Value a = evalExpr(be.left);
                if (isTruthy(a)) return Value.bool(true);
                Value b = evalExpr(be.right);
                return Value.bool(isTruthy(b));
            }

            if (be.op == ScriptRuntime.TokenKind.AND_AND) {
                Value a = evalExpr(be.left);
                if (!isTruthy(a)) return Value.bool(false);
                Value b = evalExpr(be.right);
                return Value.bool(isTruthy(b));
            }

            Value a = evalExpr(be.left);
            Value b = evalExpr(be.right);

            if (be.op == ScriptRuntime.TokenKind.PLUS) {
                if (a.kind == ValueKind.STRING || b.kind == ValueKind.STRING) {
                    return Value.string(a.asString() + b.asString());
                }
                if (a.kind == ValueKind.NUMBER && b.kind == ValueKind.NUMBER) {
                    return Value.number(a.asNumber() + b.asNumber());
                }
                throw new ScriptRuntime.ScriptError(be.pos, "Operator + not supported for " + a.kind + " and " + b.kind);
            }

            if (be.op == ScriptRuntime.TokenKind.MINUS) {
                double av = asNumberStrict(a, be.pos);
                double bv = asNumberStrict(b, be.pos);
                return Value.number(av - bv);
            }

            if (be.op == ScriptRuntime.TokenKind.STAR) {
                double av = asNumberStrict(a, be.pos);
                double bv = asNumberStrict(b, be.pos);
                return Value.number(av * bv);
            }

            if (be.op == ScriptRuntime.TokenKind.SLASH) {
                double av = asNumberStrict(a, be.pos);
                double bv = asNumberStrict(b, be.pos);
                if (bv == 0D) throw new ScriptRuntime.ScriptError(be.pos, "Division by zero");
                return Value.number(av / bv);
            }

            if (be.op == ScriptRuntime.TokenKind.PERCENT) {
                double av = asNumberStrict(a, be.pos);
                double bv = asNumberStrict(b, be.pos);
                if (bv == 0D) throw new ScriptRuntime.ScriptError(be.pos, "Modulo by zero");
                return Value.number(av % bv);
            }

            if (be.op == ScriptRuntime.TokenKind.EQEQ) {
                return Value.bool(valuesEqual(a, b));
            }
            if (be.op == ScriptRuntime.TokenKind.NEQ) {
                return Value.bool(!valuesEqual(a, b));
            }
            if (be.op == ScriptRuntime.TokenKind.LT) {
                return Value.bool(asNumberStrict(a, be.pos) < asNumberStrict(b, be.pos));
            }
            if (be.op == ScriptRuntime.TokenKind.LTE) {
                return Value.bool(asNumberStrict(a, be.pos) <= asNumberStrict(b, be.pos));
            }
            if (be.op == ScriptRuntime.TokenKind.GT) {
                return Value.bool(asNumberStrict(a, be.pos) > asNumberStrict(b, be.pos));
            }
            if (be.op == ScriptRuntime.TokenKind.GTE) {
                return Value.bool(asNumberStrict(a, be.pos) >= asNumberStrict(b, be.pos));
            }

            throw new ScriptRuntime.ScriptError(be.pos, "Unsupported operator");
        }

        if (e instanceof ScriptRuntime.UnaryExpr ue) {
            if (ue.op == ScriptRuntime.TokenKind.BANG) {
                Value r = evalExpr(ue.right);
                return Value.bool(!isTruthy(r));
            }
            if (ue.op == ScriptRuntime.TokenKind.MINUS) {
                Value r = evalExpr(ue.right);
                return Value.number(-asNumberStrict(r, ue.pos));
            }
            throw new ScriptRuntime.ScriptError(ue.pos, "Unsupported unary operator");
        }

        if (e instanceof ScriptRuntime.GetMemberExpr ge) {
            Value target = evalExpr(ge.target);
            return getMember(target, ge.member, ge.pos);
        }

        if (e instanceof ScriptRuntime.CallExpr ce) {
            Value callee = evalExpr(ce.callee);
            List<Value> args = new ArrayList<>();
            for (ScriptRuntime.Expr a : ce.args) args.add(evalExpr(a));
            return callValue(callee, args, ce.pos);
        }

        throw new ScriptRuntime.ScriptError(e.pos, "Unknown expression");
    }

    private Value invokeUserFunction(ScriptRuntime.FuncDeclStmt fn, List<Value> args, ScriptRuntime.SourcePos pos) {
        if (fn == null) return Value.nullValue();
        if (args.size() != fn.params.size()) {
            throw new ScriptRuntime.ScriptError(pos, fn.name + " expects " + fn.params.size() + " args");
        }

        if (callStack.contains(fn.name)) {
            throw new ScriptRuntime.ScriptError(pos, "Recursion not allowed: " + fn.name);
        }
        if (funcDepth >= 64) {
            throw new ScriptRuntime.ScriptError(pos, "Function depth limit");
        }

        Map<String, VarEntry> local = new HashMap<>();
        for (int i = 0; i < fn.params.size(); i++) {
            local.put(fn.params.get(i), new VarEntry(args.get(i), false));
        }

        scopes.push(local);
        callStack.push(fn.name);
        funcDepth++;
        try {
            if (fn.body != null) {
                for (ScriptRuntime.Stmt st : fn.body) {
                    if (!isRunning.getAsBoolean()) break;
                    evalStatement(st);
                }
            }
            return Value.nullValue();
        } catch (ReturnSignal r) {
            return r.value == null ? Value.nullValue() : r.value;
        } finally {
            funcDepth = Math.max(0, funcDepth - 1);
            callStack.pop();
            scopes.pop();
        }
    }

    private Map<String, VarEntry> currentScope() {
        Map<String, VarEntry> m = scopes.peek();
        return m == null ? vars : m;
    }

    private VarEntry findVar(String name) {
        if (name == null) return null;
        for (Map<String, VarEntry> m : scopes) {
            VarEntry v = m.get(name);
            if (v != null) return v;
        }
        return null;
    }

    private Map<String, VarEntry> findVarScope(String name) {
        if (name == null) return null;
        for (Map<String, VarEntry> m : scopes) {
            if (m.containsKey(name)) return m;
        }
        return null;
    }

    private void assignVar(String name, Value value, ScriptRuntime.SourcePos pos) {
        Map<String, VarEntry> m = findVarScope(name);
        if (m == null) {
            throw new ScriptRuntime.ScriptError(pos, "Unknown identifier: " + name);
        }
        VarEntry entry = m.get(name);
        if (entry != null && entry.isConst) {
            throw new ScriptRuntime.ScriptError(pos, "Cannot reassign const: " + name);
        }
        m.put(name, new VarEntry(value, false));
    }

    private static boolean isTruthy(Value v) {
        if (v == null) return false;
        return switch (v.kind) {
            case NULL -> false;
            case BOOL -> v.boolVal;
            case NUMBER -> v.numVal != 0;
            case STRING -> v.strVal != null && !v.strVal.isEmpty();
            default -> true;
        };
    }

    private static double asNumberStrict(Value v, ScriptRuntime.SourcePos pos) {
        if (v.kind == ValueKind.NUMBER) return v.numVal;
        if (v.kind == ValueKind.BOOL) return v.boolVal ? 1D : 0D;
        if (v.kind == ValueKind.STRING) {
            try {
                return Double.parseDouble(v.strVal);
            } catch (Exception ignored) {
                throw new ScriptRuntime.ScriptError(pos, "Expected number");
            }
        }
        throw new ScriptRuntime.ScriptError(pos, "Expected number");
    }

    private static boolean valuesEqual(Value a, Value b) {
        if (a.kind == ValueKind.NULL && b.kind == ValueKind.NULL) return true;
        if ((a.kind == ValueKind.NUMBER || a.kind == ValueKind.BOOL) && (b.kind == ValueKind.NUMBER || b.kind == ValueKind.BOOL)) {
            return a.asNumber() == b.asNumber();
        }
        if (a.kind == ValueKind.STRING || b.kind == ValueKind.STRING) {
            return a.asString().equals(b.asString());
        }
        if (a.kind != b.kind) return false;
        return switch (a.kind) {
            case TAG -> a.tag == b.tag;
            case ITEM -> a.item == b.item;
            case ITEM_MAP -> a.itemMap == b.itemMap;
            case PACKET -> a.packet == b.packet;
            case FUNCTION -> a.fn == b.fn;
            case NAMESPACE_CREATE, NAMESPACE_DEBUG, NAMESPACE_NBT, NAMESPACE_ENV, NAMESPACE_CLICKTYPE, NAMESPACE_PACKET -> true;
            default -> false;
        };
    }

    private Value getMember(Value target, String name, ScriptRuntime.SourcePos pos) {
        if (target.kind == ValueKind.NAMESPACE_CREATE) {
            return Value.function((args, p) -> callCreate(name, args, p));
        }
        if (target.kind == ValueKind.NAMESPACE_DEBUG) {
            if ("print".equals(name)) {
                return Value.function((args, p) -> {
                    String msg = args.isEmpty() ? "" : args.get(0).asString();
                    if (log != null) log.accept(msg);
                    return Value.nullValue();
                });
            }
            throw new ScriptRuntime.ScriptError(pos, "Unknown debug member: " + name);
        }

        if (target.kind == ValueKind.NAMESPACE_NBT) {
            if ("fromJson".equals(name)) {
                return Value.function((args, p) -> {
                    if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "nbt.fromJson(text) expects 1 arg");
                    String src = args.get(0).asString();
                    if (src == null) src = "";
                    try {
                        try {
                            return Value.tag(TagParser.parseTag(src));
                        } catch (Exception ignored) {
                        }
                        JsonElement el = JsonParser.parseString(src);
                        Tag t = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, el);
                        return Value.tag(t);
                    } catch (Exception ex) {
                        throw new ScriptRuntime.ScriptError(p, ex.getMessage() == null ? "Invalid JSON" : ex.getMessage());
                    }
                });
            }
            throw new ScriptRuntime.ScriptError(pos, "Unknown nbt member: " + name);
        }

        if (target.kind == ValueKind.NAMESPACE_ENV) {
            Minecraft mc = Minecraft.getInstance();
            if ("player".equals(name)) {
                if (mc.player == null) return Value.nullValue();
                return Value.string(mc.player.getName().getString());
            }
            if ("containerId".equals(name)) {
                if (mc.player == null) return Value.nullValue();
                return Value.number(mc.player.containerMenu.containerId);
            }
            if ("stateId".equals(name)) {
                if (mc.player == null) return Value.nullValue();
                return Value.number(mc.player.containerMenu.getStateId());
            }
            if ("tick".equals(name)) {
                if (mc.level == null) return Value.nullValue();
                return Value.number(mc.level.getGameTime());
            }
            throw new ScriptRuntime.ScriptError(pos, "Unknown env member: " + name);
        }

        if (target.kind == ValueKind.NAMESPACE_CLICKTYPE) {
            if ("values".equals(name)) {
                return Value.function((args, p) -> {
                    if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(p, "ClickType.values() expects 0 args");
                    ListTag out = new ListTag();
                    for (ClickType ct : ClickType.values()) {
                        out.add(StringTag.valueOf(ct.name()));
                    }
                    return Value.tag(out);
                });
            }
            return Value.string(name);
        }

        if (target.kind == ValueKind.NAMESPACE_PACKET) {
            if ("pickup".equals(name)) {
                return Value.function((args, p) -> {
                    if (args.isEmpty() || args.size() > 2)
                        throw new ScriptRuntime.ScriptError(p, "packet.pickup(slot[,button])");
                    int slot = args.get(0).asInt(p);
                    int btn = args.size() == 2 ? args.get(1).asInt(p) : 0;
                    return makeSimpleClickPacket(p, slot, btn, ClickType.PICKUP);
                });
            }
            if ("quickMove".equals(name)) {
                return Value.function((args, p) -> {
                    if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "packet.quickMove(slot)");
                    int slot = args.get(0).asInt(p);
                    return makeSimpleClickPacket(p, slot, 0, ClickType.QUICK_MOVE);
                });
            }
            if ("throw".equals(name)) {
                return Value.function((args, p) -> {
                    if (args.isEmpty() || args.size() > 2)
                        throw new ScriptRuntime.ScriptError(p, "packet.throw(slot[,button])");
                    int slot = args.get(0).asInt(p);
                    int btn = args.size() == 2 ? args.get(1).asInt(p) : 0;
                    return makeSimpleClickPacket(p, slot, btn, ClickType.THROW);
                });
            }
            throw new ScriptRuntime.ScriptError(pos, "Unknown packet member: " + name);
        }

        if ("toString".equals(name)) {
            return Value.function((args, p) -> Value.string(target.asString()));
        }

        if (target.kind == ValueKind.STRING) {
            if ("repeat".equals(name)) {
                String base = target.asString();
                return Value.function((args, p) -> {
                    if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "string.repeat expects 1 arg");
                    int n = args.get(0).asInt(p);
                    if (n <= 0) return Value.string("");
                    long total = (long) base.length() * (long) n;
                    if (total > 1_000_000L) throw new ScriptRuntime.ScriptError(p, "repeat too large");
                    return Value.string(base.repeat(n));
                });
            }
        }

        if (target.kind == ValueKind.TAG && target.tag != null) {
            if ("toJson".equals(name)) {
                Tag t = target.tag;
                return Value.function((args, p) -> {
                    if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(p, "tag.toJson() expects 0 args");
                    JsonElement el = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, t);
                    String json = new GsonBuilder().setPrettyPrinting().create().toJson(el);
                    return Value.string(json);
                });
            }

            if ("add".equals(name)) {
                Tag t = target.tag;
                return Value.function((args, p) -> {
                    if (t instanceof CompoundTag ct) {
                        if (args.size() != 2) throw new ScriptRuntime.ScriptError(p, "compoundTag.add expects 2 args");
                        String k = args.get(0).asString();
                        Tag v = toTag(args.get(1), p);
                        try {
                            ct.put(k, v);
                        } catch (Exception ex) {
                            throw new ScriptRuntime.ScriptError(p, ex.getMessage() == null ? "Failed to add" : ex.getMessage());
                        }
                        return target;
                    }
                    if (t instanceof ListTag lt) {
                        if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "listTag.add expects 1 arg");
                        try {
                            lt.add(toTag(args.get(0), p));
                        } catch (Exception ex) {
                            throw new ScriptRuntime.ScriptError(p, ex.getMessage() == null ? "Failed to add" : ex.getMessage());
                        }
                        return target;
                    }
                    throw new ScriptRuntime.ScriptError(p, "add not supported for tag type");
                });
            }

            Tag t = target.tag;
            if (t instanceof CompoundTag ct) {
                if ("has".equals(name)) {
                    return Value.function((args, p) -> {
                        if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "compoundTag.has expects 1 arg");
                        String k = args.get(0).asString();
                        return Value.bool(ct.contains(k));
                    });
                }
                if ("get".equals(name)) {
                    return Value.function((args, p) -> {
                        if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "compoundTag.get expects 1 arg");
                        String k = args.get(0).asString();
                        Tag got = ct.get(k);
                        return got == null ? Value.nullValue() : Value.tag(got);
                    });
                }
                if ("remove".equals(name)) {
                    return Value.function((args, p) -> {
                        if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "compoundTag.remove expects 1 arg");
                        String k = args.get(0).asString();
                        Tag got = ct.get(k);
                        ct.remove(k);
                        return got == null ? Value.nullValue() : Value.tag(got);
                    });
                }
                if ("size".equals(name)) {
                    return Value.function((args, p) -> {
                        if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(p, "compoundTag.size expects 0 args");
                        return Value.number(ct.size());
                    });
                }
            }

            if (t instanceof ListTag lt) {
                if ("size".equals(name)) {
                    return Value.function((args, p) -> {
                        if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(p, "listTag.size expects 0 args");
                        return Value.number(lt.size());
                    });
                }
                if ("get".equals(name)) {
                    return Value.function((args, p) -> {
                        if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "listTag.get expects 1 arg");
                        int idx = args.get(0).asInt(p);
                        if (idx < 0 || idx >= lt.size()) return Value.nullValue();
                        return Value.tag(lt.get(idx));
                    });
                }
                if ("remove".equals(name)) {
                    return Value.function((args, p) -> {
                        if (args.size() != 1) throw new ScriptRuntime.ScriptError(p, "listTag.remove expects 1 arg");
                        int idx = args.get(0).asInt(p);
                        if (idx < 0 || idx >= lt.size()) return Value.nullValue();
                        return Value.tag(lt.remove(idx));
                    });
                }
            }
        }

        if (target.kind == ValueKind.ITEM && target.item != null) {
            if ("clone".equals(name)) {
                return Value.function((args, p) -> {
                    if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(p, "item.clone() expects 0 args");
                    return Value.item(target.item.copy());
                });
            }
        }

        if (target.kind == ValueKind.ITEM_MAP && target.itemMap != null) {
            if ("add".equals(name)) {
                return Value.function((args, p) -> {
                    if (args.size() != 2) throw new ScriptRuntime.ScriptError(p, "itemMap.add expects 2 args");
                    int slot = args.get(0).asInt(p);
                    ItemStack it = args.get(1).asItem(p);
                    target.itemMap.put(slot, it.copy());
                    return target;
                });
            }
        }

        throw new ScriptRuntime.ScriptError(pos, "Unknown member: " + name + " on " + target.kind);
    }

    private Value makeSimpleClickPacket(ScriptRuntime.SourcePos pos, int slot, int button, ClickType clickType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) throw new ScriptRuntime.ScriptError(pos, "No player");
        int cid = mc.player.containerMenu.containerId;
        int sid = mc.player.containerMenu.getStateId();
        return Value.packet(new ServerboundContainerClickPacket(cid, sid, slot, button, clickType, ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
    }

    private Value callValue(Value callee, List<Value> args, ScriptRuntime.SourcePos pos) {
        if (callee.kind != ValueKind.FUNCTION || callee.fn == null) {
            throw new ScriptRuntime.ScriptError(pos, "Not callable: " + callee.kind);
        }
        return callee.fn.invoke(args, pos);
    }

    private Value callCreate(String name, List<Value> args, ScriptRuntime.SourcePos pos) {
        switch (name) {
            case "compoundTag" -> {
                if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(pos, "create.compoundTag takes 0 args");
                return Value.tag(new CompoundTag());
            }
            case "listTag" -> {
                if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(pos, "create.listTag takes 0 args");
                return Value.tag(new ListTag());
            }
            case "stringTag" -> {
                if (args.size() != 1) throw new ScriptRuntime.ScriptError(pos, "create.stringTag takes 1 arg");
                return Value.tag(StringTag.valueOf(args.get(0).asString()));
            }
            case "intTag" -> {
                if (args.size() != 1) throw new ScriptRuntime.ScriptError(pos, "create.intTag takes 1 arg");
                return Value.tag(IntTag.valueOf(args.get(0).asInt(pos)));
            }
            case "doubleTag" -> {
                if (args.size() != 1) throw new ScriptRuntime.ScriptError(pos, "create.doubleTag takes 1 arg");
                return Value.tag(DoubleTag.valueOf(args.get(0).asNumber()));
            }
            case "byteTag" -> {
                if (args.size() != 1) throw new ScriptRuntime.ScriptError(pos, "create.byteTag takes 1 arg");
                Value v = args.get(0);
                if (v.kind == ValueKind.BOOL) return Value.tag(ByteTag.valueOf(v.boolVal));
                return Value.tag(ByteTag.valueOf((byte) v.asInt(pos)));
            }
            case "item" -> {
                if (args.size() < 2 || args.size() > 3)
                    throw new ScriptRuntime.ScriptError(pos, "create.item(id,count[,tag])");
                String id = args.get(0).asString();
                int cnt = args.get(1).asInt(pos);
                CompoundTag tag = null;
                if (args.size() == 3) {
                    Tag t = toTag(args.get(2), pos);
                    if (t instanceof CompoundTag ct) tag = ct;
                    else throw new ScriptRuntime.ScriptError(pos, "item tag must be compoundTag");
                }

                Item item = Items.STONE;
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    item = BuiltInRegistries.ITEM.get(rl);
                }
                ItemStack st = new ItemStack(item, Math.max(1, Math.min(cnt, 64)));
                if (tag != null) st.setTag(tag.copy());
                return Value.item(st);
            }
            case "itemMap" -> {
                if (!args.isEmpty()) throw new ScriptRuntime.ScriptError(pos, "create.itemMap takes 0 args");
                return Value.itemMap(new Int2ObjectOpenHashMap<>());
            }
            case "containerClick" -> {
                if (args.size() != 7)
                    throw new ScriptRuntime.ScriptError(pos, "create.containerClick(cid,sid,slot,btn,clickType,carried,changedSlots)");
                int cid = args.get(0).asInt(pos);
                int sid = args.get(1).asInt(pos);
                int slot = args.get(2).asInt(pos);
                int btn = args.get(3).asInt(pos);
                ClickType ct = parseClickType(args.get(4).asString());
                ItemStack carried = args.get(5).asItem(pos);
                Int2ObjectOpenHashMap<ItemStack> map = args.get(6).asItemMap(pos);
                return Value.packet(new ServerboundContainerClickPacket(cid, sid, slot, btn, ct, carried.copy(), map));
            }
        }
        throw new ScriptRuntime.ScriptError(pos, "Unknown create function: " + name);
    }

    private Tag toTag(Value v, ScriptRuntime.SourcePos pos) {
        if (v.kind == ValueKind.TAG && v.tag != null) return v.tag;
        if (v.kind == ValueKind.STRING) return StringTag.valueOf(v.strVal);
        if (v.kind == ValueKind.NUMBER) {
            double d = v.numVal;
            if (Math.floor(d) == d && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                return IntTag.valueOf((int) d);
            }
            return DoubleTag.valueOf(d);
        }
        if (v.kind == ValueKind.BOOL) return ByteTag.valueOf(v.boolVal);
        throw new ScriptRuntime.ScriptError(pos, "Cannot convert to tag: " + v.kind);
    }

    private boolean isClickTypeName(String s) {
        if (s == null) return false;
        try {
            ClickType.valueOf(s.trim().toUpperCase());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static ClickType parseClickType(String s) {
        if (s == null) return ClickType.PICKUP;
        try {
            return ClickType.valueOf(s.trim().toUpperCase());
        } catch (Exception ignored) {
            return ClickType.PICKUP;
        }
    }

    private record VarEntry(Value value, boolean isConst) {
    }

    record Value(ValueKind kind, boolean boolVal, double numVal, String strVal, Tag tag, ItemStack item,
                 Int2ObjectOpenHashMap<ItemStack> itemMap, ServerboundContainerClickPacket packet,
                 ScriptRuntime.Fn fn) {

        static Value nullValue() {
            return new Value(ValueKind.NULL, false, 0, null, null, null, null, null, null);
        }

        static Value bool(boolean v) {
            return new Value(ValueKind.BOOL, v, 0, null, null, null, null, null, null);
        }

        static Value number(double v) {
            return new Value(ValueKind.NUMBER, false, v, null, null, null, null, null, null);
        }

        static Value string(String v) {
            return new Value(ValueKind.STRING, false, 0, v == null ? "" : v, null, null, null, null, null);
        }

        static Value tag(Tag t) {
            return new Value(ValueKind.TAG, false, 0, null, t, null, null, null, null);
        }

        static Value item(ItemStack i) {
            return new Value(ValueKind.ITEM, false, 0, null, null, i, null, null, null);
        }

        static Value itemMap(Int2ObjectOpenHashMap<ItemStack> m) {
            return new Value(ValueKind.ITEM_MAP, false, 0, null, null, null, m, null, null);
        }

        static Value packet(ServerboundContainerClickPacket p) {
            return new Value(ValueKind.PACKET, false, 0, null, null, null, null, p, null);
        }

        static Value function(ScriptRuntime.Fn fn) {
            return new Value(ValueKind.FUNCTION, false, 0, null, null, null, null, null, fn);
        }

        static Value createNamespace() {
            return new Value(ValueKind.NAMESPACE_CREATE, false, 0, null, null, null, null, null, null);
        }

        static Value debugNamespace() {
            return new Value(ValueKind.NAMESPACE_DEBUG, false, 0, null, null, null, null, null, null);
        }

        static Value nbtNamespace() {
            return new Value(ValueKind.NAMESPACE_NBT, false, 0, null, null, null, null, null, null);
        }

        static Value envNamespace() {
            return new Value(ValueKind.NAMESPACE_ENV, false, 0, null, null, null, null, null, null);
        }

        static Value clickTypeNamespace() {
            return new Value(ValueKind.NAMESPACE_CLICKTYPE, false, 0, null, null, null, null, null, null);
        }

        static Value packetNamespace() {
            return new Value(ValueKind.NAMESPACE_PACKET, false, 0, null, null, null, null, null, null);
        }

        String asString() {
            return switch (kind) {
                case NULL -> "null";
                case BOOL -> String.valueOf(boolVal);
                case NUMBER -> (Math.floor(numVal) == numVal ? String.valueOf((long) numVal) : String.valueOf(numVal));
                case STRING -> strVal == null ? "" : strVal;
                case TAG -> tag == null ? "<null>" : tag.getAsString();
                case ITEM -> item == null ? "<null>" : (item.getCount() + "x " + item.getItem());
                case ITEM_MAP -> itemMap == null ? "<null>" : ("itemMap(" + itemMap.size() + ")");
                case PACKET -> packet == null ? "<null>" : packet.toString();
                case FUNCTION -> "<function>";
                case NAMESPACE_CREATE -> "create";
                case NAMESPACE_DEBUG -> "debug";
                case NAMESPACE_NBT -> "nbt";
                case NAMESPACE_ENV -> "env";
                case NAMESPACE_CLICKTYPE -> "ClickType";
                case NAMESPACE_PACKET -> "packet";
            };
        }

        double asNumber() {
            if (kind == ValueKind.NUMBER) return numVal;
            if (kind == ValueKind.BOOL) return boolVal ? 1 : 0;
            if (kind == ValueKind.STRING) {
                try {
                    return Double.parseDouble(strVal);
                } catch (Exception ignored) {
                    return 0;
                }
            }
            return 0;
        }

        int asInt(ScriptRuntime.SourcePos pos) {
            if (kind == ValueKind.NUMBER) return (int) Math.round(numVal);
            if (kind == ValueKind.BOOL) return boolVal ? 1 : 0;
            if (kind == ValueKind.STRING) {
                try {
                    return Integer.parseInt(strVal);
                } catch (Exception ignored) {
                    throw new ScriptRuntime.ScriptError(pos, "Expected int");
                }
            }
            throw new ScriptRuntime.ScriptError(pos, "Expected int");
        }

        ItemStack asItem(ScriptRuntime.SourcePos pos) {
            if (kind == ValueKind.ITEM && item != null) return item;
            throw new ScriptRuntime.ScriptError(pos, "Expected item");
        }

        Int2ObjectOpenHashMap<ItemStack> asItemMap(ScriptRuntime.SourcePos pos) {
            if (kind == ValueKind.ITEM_MAP && itemMap != null) return itemMap;
            throw new ScriptRuntime.ScriptError(pos, "Expected itemMap");
        }
    }

    private enum ValueKind {
        NULL,
        BOOL,
        NUMBER,
        STRING,
        TAG,
        ITEM,
        ITEM_MAP,
        PACKET,
        FUNCTION,
        NAMESPACE_CREATE,
        NAMESPACE_DEBUG,
        NAMESPACE_NBT,
        NAMESPACE_ENV,
        NAMESPACE_CLICKTYPE,
        NAMESPACE_PACKET
    }
}
