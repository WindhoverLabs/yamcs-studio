/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.yamcs.studio.data.formula.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.yamcs.studio.data.formula.FormulaFunction;
import org.yamcs.studio.data.vtype.VString;
import org.yamcs.studio.data.vtype.VStringArray;
import org.yamcs.studio.data.vtype.ValueFactory;

class ArrayOfStringFormulaFunction implements FormulaFunction {

    @Override
    public boolean isVarArgs() {
        return true;
    }

    @Override
    public String getName() {
        return "arrayOf";
    }

    @Override
    public String getDescription() {
        return "Constructs array from a series of string";
    }

    @Override
    public List<Class<?>> getArgumentTypes() {
        return Arrays.<Class<?>> asList(VString.class);
    }

    @Override
    public List<String> getArgumentNames() {
        return Arrays.asList("strArgs");
    }

    @Override
    public Class<?> getReturnType() {
        return VStringArray.class;
    }

    @Override
    public Object calculate(List<Object> args) {

        List<String> data = new ArrayList<>();
        for (Object arg : args) {
            VString str = (VString) arg;
            if (str == null || str.getValue() == null) {
                data.add(null);
            } else {
                data.add(str.getValue());
            }
        }

        return ValueFactory.newVStringArray(data,
                highestSeverityOf(args, false),
                latestValidTimeOrNowOf(args));
    }
}
