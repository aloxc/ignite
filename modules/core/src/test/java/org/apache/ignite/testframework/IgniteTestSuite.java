/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.testframework;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.ignite.testsuites.IgniteIgnore;
import org.jetbrains.annotations.Nullable;
import org.junit.internal.MethodSorter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for run junit tests.
 * Test methods marked with @Ignored annotation won't be executed.
 */
public class IgniteTestSuite extends TestSuite {
    /** Whether to execute only ignored tests. */
    private final boolean ignoredOnly;

    /**
     * Constructor.
     *
     * @param name Name.
     */
    public IgniteTestSuite(String name) {
        this(null, name);
    }

    /**
     * Constructor.
     *
     * @param theClass TestCase class
     */
    public IgniteTestSuite(Class<? extends TestCase> theClass) {
        this(theClass, false);
    }

    /**
     * Constructor.
     *
     * @param theClass TestCase class
     * @param ignoredOnly Whether to execute only ignored tests.
     */
    public IgniteTestSuite(Class<? extends TestCase> theClass, boolean ignoredOnly) {
        this(theClass, null, ignoredOnly);
    }

    /**
     * Constructor.
     *
     * @param theClass TestCase class
     * @param name Test suite name.
     */
    public IgniteTestSuite(Class<? extends TestCase> theClass, String name) {
        this(theClass, name, false);
    }

    /**
     * Constructor.
     *
     * @param theClass TestCase class
     * @param name Test suite name.
     * @param ignoredOnly Whether to execute only ignored tests.
     */
    public IgniteTestSuite(@Nullable Class<? extends TestCase> theClass, @Nullable String name, boolean ignoredOnly) {
        this.ignoredOnly = ignoredOnly;

        if (theClass != null)
            addTestsFromTestCase(theClass);

        if (name != null)
            setName(name);
    }

    /** {@inheritDoc} */
    @Override public void addTestSuite(Class<? extends TestCase> testClass) {
        addTest(new IgniteTestSuite(testClass, ignoredOnly));
    }

    /**
     *
     * @param theClass TestCase class
     */
    private void addTestsFromTestCase(Class<?> theClass) {
        setName(theClass.getName());

        try {
            getTestConstructor(theClass);
        }
        catch (NoSuchMethodException ex) {
            addTest(warning("Class " + theClass.getName() +
                " has no public constructor TestCase(String name) or TestCase()"));

            return;
        }

        if(!Modifier.isPublic(theClass.getModifiers()))
            addTest(warning("Class " + theClass.getName() + " is not public"));
        else {
            Class superCls = theClass;

            int testAdded = 0;

            for(List<String> names = new ArrayList<>(); Test.class.isAssignableFrom(superCls);
                superCls = superCls.getSuperclass()) {
                Method[] methods = MethodSorter.getDeclaredMethods(superCls);

                for (Method each : methods) {
                    if (addTestMethod(each, names, theClass))
                        testAdded++;
                }
            }

            if(testAdded == 0)
                addTest(warning("No tests found in " + theClass.getName()));
        }
    }

    /**
     * Add test method.
     *
     * @param m Test method.
     * @param names Test name list.
     * @param theClass Test class.
     * @return Whether test method was added.
     */
    private boolean addTestMethod(Method m, List<String> names, Class<?> theClass) {
        String name = m.getName();

        if (names.contains(name))
            return false;

        if (!isPublicTestMethod(m)) {
            if (isTestMethod(m))
                addTest(warning("Test method isn't public: " + m.getName() + "(" + theClass.getCanonicalName() + ")"));

            return false;
        }

        names.add(name);

        if (m.isAnnotationPresent(IgniteIgnore.class) == ignoredOnly) {
            addTest(createTest(theClass, name));

            return true;
        }

        return false;
    }

    /**
     * Check whether this is a test method.
     *
     * @param m Method.
     * @return {@code True} if this is a test method.
     */
    private static boolean isTestMethod(Method m) {
        return m.getParameterTypes().length == 0 &&
            m.getName().startsWith("test") &&
            m.getReturnType().equals(Void.TYPE);
    }

    /**
     * Check whether this is a public test method.
     *
     * @param m Method.
     * @return {@code True} if this is a public test method.
     */
    private static boolean isPublicTestMethod(Method m) {
        return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
    }
}