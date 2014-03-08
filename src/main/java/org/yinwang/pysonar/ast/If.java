package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar._;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.HashSet;
import java.util.Set;


public class If extends Node {

    @NotNull
    public Node test;
    public Node body;
    public Node orelse;

    public static Set<String> dateTimeBool = new HashSet<>();


    public If(@NotNull Node test, Node body, Node orelse, String file, int start, int end) {
        super(file, start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        // ignore condition for now
        Type tt = transformExpr(test, s);
        if (Type.contains(tt, Analyzer.self.builtins.Datetime_time)) {
            String loc = this.file + ": " + test.start;
            if (!dateTimeBool.contains(loc)) {
                dateTimeBool.add(loc);
                _.msg("\n*** [WARNING " + dateTimeBool.size() + "] datetime.time used as boolean! \n" +
                        "- file: " + this.file +
                        "\n- character offset: " + test.start + "\n");
//                _.msg("- call stack: ");
//                int count = 0;
//                for (Object o : Analyzer.self.callStack) {
//                    if (o instanceof Call) {
//                        Call call = (Call) o;
//                        _.msg(call.func.name + " @ " + call.file + ":" + call.start);
//                    }
//                    count++;
//                    if (count == 10) {
//                        break;
//                    }
//                }
            }
        }

        if (body != null) {
            type1 = transformExpr(body, s1);
        } else {
            type1 = Type.CONT;
        }

        if (orelse != null) {
            type2 = transformExpr(orelse, s2);
        } else {
            type2 = Type.CONT;
        }

        boolean cont1 = UnionType.contains(type1, Type.CONT);
        boolean cont2 = UnionType.contains(type2, Type.CONT);

        // decide which branch affects the downstream state
        if (cont1 && cont2) {
            s.overwrite(State.merge(s1, s2));
        } else if (cont1) {
            s.overwrite(s1);
        } else if (cont2) {
            s.overwrite(s2);
        }

        return UnionType.union(type1, type2);
    }


    @NotNull
    @Override
    public String toString() {
        return "<If:" + start + ":" + test + ":" + body + ":" + orelse + ">";
    }

}
