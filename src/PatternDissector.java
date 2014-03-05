import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
import java.util.Arrays;

import java.util.regex.Pattern;

class PatternDissector {
    
    enum LoggingLevel {
        SIMPLE,
        VERBOSE;
    }
    
    private static LoggingLevel loggingLevel = LoggingLevel.VERBOSE;
    
    private static final transient Map<String, Class> patterns;
    
    static {
        Class[] innerClasses = Pattern.class.getDeclaredClasses();
        Map<String, Class> m = new HashMap<String, Class>();
        
        for (Class c: innerClasses) {
            m.put(c.getSimpleName(), c);
        }
        
        patterns = Collections.unmodifiableMap(m);
    }
    
    private static final Class<?> Node = patterns.get("Node");
    
    /**
     * Get declared field by name in a class and make it accessible.
     */
    private static Field getDeclaredField(Class clazz, String name) {
        Field field;
        try {
            field = clazz.getDeclaredField(name);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unexpected inner structure of " + clazz.getName() + ". " + name + " field not found");
        }
        
        return field;
    }
    
    /**
     * Get declared method by name and make it accessible.
     */
    private static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Method method;
        try {
            method = clazz.getDeclaredMethod(name, paramTypes);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unexpected inner structure of " + clazz.getName() + ". " + name + " method not found");
        }
        
        return method;
    }
    
    private static Object invoke(Method method, Object target, Object... args) {
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException|IllegalAccessException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        
        return result;
    }
    
    /*
    private static final transient Map<Object, String> charProps;
    // private static final transient List<String> propNames;
    
    static {
        Class charPropertyNamesClass = patterns.get("CharPropertyNames");
        Field mapField = getDeclaredField(charPropertyNamesClass, "map");
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) mapField.get(charPropertyNamesClass);
            
            Map<Object, String> rev = new HashMap<Object, String>();
            
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                // rev.put(entry.getValue(), entry.getKey());
                //  System.out.println(entry.getValue());
                
                Object charPropFactory = entry.getValue();
                
                System.out.println(charPropFactory.getClass().getEnclosingConstructor());
                System.out.println(charPropFactory.getClass().getEnclosingClass());
                
                Method makeMethod = getDeclaredMethod(charPropFactory.getClass(), "make");
                Object charProp = invoke(makeMethod, charPropFactory);
                assert(charProp != null);
                
                // rev.put(charProp, entry.getKey());
                System.out.println(charProp.getClass());
                
                charProp = invoke(makeMethod, charPropFactory);
                System.out.println(charProp.getClass());
                
                System.out.println(charProp.getClass().getEnclosingConstructor());
                System.out.println(charProp.getClass().getEnclosingClass());
                System.out.println(charProp.getClass().getSuperclass());
                System.out.println();
                
            }
            
           charProps = Collections.unmodifiableMap(rev);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        
    }
    */
    
    private static final Map<String, String> INFO_FORMAT_STRINGS_VERBOSE;
    static {
        Map<String, String> m = new HashMap<String, String>();
        
        m.put("Start", "%s. Start unanchored match (minLength=%d)\n");
        m.put("StartS", "%s. Start unanchored match with support for supplementary characters (minLength=%d)\n");
        
        m.put("Caret", "%s. Match beginning of a line: (?m:^)\n");
        m.put("UnixCaret", "%s. Match beginning of a line in UNIX_LINES mode: (?dm:^)\n");
        m.put("Dollar(multiline=true)", "%s. Match before a line terminator (end of a line) or the end of the string: (?m:$)\n");
        m.put("Dollar(multiline=false)", "%s. Match the end of the string but just before final line terminator if any: \\Z or default $\n");
        m.put("UnixDollar(multiline=true)", "%s. Match before a line terminator (end of a line) or the end of the string, in UNIX_LINES mode: (?dm:$)\n");
        m.put("UnixDollar(multiline=false)",  "%s. Match the end of the string but just before final line terminator if any, in UNIX_LINES mode: (?d:\\Z) or (?d:$)\n");
        m.put("Begin", "%s. Match the beginning of a string: \\A or default ^\n");
        m.put("End", "%s. Match the end of a string: \\z\n");
        m.put("LastMatch", "%s. Last match boundary: \\G\n");
        
        m.put("Single", "%s. Match a BMP (Basic Multilingual Plane) character (code point at and below 0xFFFF): U+%04X %s\n");
        m.put("SingleS", "%s. Match a supplementary character (code point at and above 0x10000): U+%04X %s\n");
        m.put("SingleU", "%s. Match a Unicode character case-insensitively with simple 1:1 case-folding: (?iu). Lowercase code point: U+%04X %s\n");
                  
        m.put("Slice", "%s.\n");
        m.put("SliceS", "%s.\n");
        m.put("BnM", "%s. Optimized matching with Boyer-Moore search algorithm (BMP only version) (length=%d)\n");
        m.put("BnMS", "%s. Optimized matching with Boyer-Moore search algorithm (supplementary version) (length=%d, lengthChar=%d)\n");
        
        m.put("Branch", "%s. Attempt the following %d alternatives in printed order:\n");
        m.put("BranchConn", "%s.\n");
        
        m.put("Node", "%s. Accept match\n");
        
        m.put("Ctype", "%s. Match POSIX character class %s (US-ASCII)\n");
        m.put("BitClass", "%s. Optimized character class with boolean[] to match characters in Latin-1 (code point <= 255). Match any of the following %d character(s):\n");
                      
        m.put("CharProperty.complement", "%s (character class negation). Match any character NOT matched by the following character class:\n");
        m.put("Pattern.setDifference", "%s (character class subtraction). Match any character matched by the 1st character class, but NOT the 2nd character class:\n");
        m.put("Pattern.union", "%s (character class union). Match any character matched by either character classes below:\n");
        m.put("Pattern.intersection", "%s (character class intersection). Match any character matched by both character classes below:\n");
        
        m.put("Pattern.rangeFor", "%s (character range). Match any character within the range from code point U+%04X to code point U+%04X (both ends inclusive)\n");
        
        INFO_FORMAT_STRINGS_VERBOSE = Collections.unmodifiableMap(m);
    }
    
    private static final Map<String, String> INFO_FORMAT_STRINGS_SIMPLE;
    static {
        Map<String, String> m = new HashMap<String, String>();
        
        m.put("Start", "%s. Start unanchored match (minLength=%d)\n");
        m.put("StartS", "%s. Start unanchored match (minLength=%d)\n");
        
        m.put("Caret", "%s. (?m:^)\n");
        m.put("UnixCaret", "%s. (?dm:^)\n");
        m.put("Dollar(multiline=true)", "%s. (?m:$)\n");
        m.put("Dollar(multiline=false)", "%s. \\Z or default $\n");
        m.put("UnixDollar(multiline=true)", "%s. (?dm:$)\n");
        m.put("UnixDollar(multiline=false)",  "%s. (?d:\\Z) or (?d:$)\n");
        m.put("Begin", "%s. \\A or default ^\n");
        m.put("End", "%s. \\z\n");
        m.put("LastMatch", "%s. \\G\n");
        
        m.put("Single", "%s. Match code point: U+%04X %s\n");
        m.put("SingleS", "%s. Match code point: U+%04X %s\n");
        m.put("SingleU", "%s. Caseless match. Lowercase code point: U+%04X %s\n");
                  
        m.put("Slice", "%s.\n");
        m.put("SliceS", "%s.\n");
        m.put("BnM", "%s. Boyer-Moore (BMP only version) (length=%d)\n");
        m.put("BnMS", "%s. Boyer-Moore (supplementary version) (length=%d, lengthChar=%d)\n");
        
        m.put("Branch", "%s. Alternation (in printed order):\n");
        m.put("BranchConn", "%s.\n");
        
        m.put("Node", "%s. Accept match\n");
        
        m.put("Ctype", "%s. POSIX (US-ASCII): %s\n");
        m.put("BitClass", "%s. Match any of these %d character(s):\n");
        
        m.put("CharProperty.complement", "%s. A\u0304:\n");
        m.put("Pattern.setDifference", "%s. A \u2216 B:\n");
        m.put("Pattern.union", "%s. A \u222a B:\n");
        m.put("Pattern.intersection", "%s. A \u2229 B:\n");
        
        m.put("Pattern.rangeFor", "%s. U+%04X <= codePoint <= U+%04X.\n");
        
        INFO_FORMAT_STRINGS_SIMPLE = Collections.unmodifiableMap(m);
    }
    
    private static void info(int depth, String className, Object... params) {
        Object[] args = new Object[params.length + 1];
        
        args[0] = className;
        System.arraycopy(params, 0, args, 1, params.length);
        
        indent(depth);
        switch (loggingLevel) {
            case SIMPLE:
                System.out.printf(INFO_FORMAT_STRINGS_SIMPLE.get(className), args);
                break;
            case VERBOSE:
                System.out.printf(INFO_FORMAT_STRINGS_VERBOSE.get(className), args);
                break;
        }
    }
    
    private static boolean isPrintableCodePoint(int codePoint) {
        switch (Character.getType(codePoint)) {
            case Character.CONTROL: // Cc
            case Character.FORMAT: // Cf
            case Character.PRIVATE_USE: // Co
            case Character.SURROGATE: // Cs
                // A paired surrogate does not belong to Cs category, since we are working on code point
            case Character.UNASSIGNED: // Cn
                
            case Character.LINE_SEPARATOR: // Zl
            case Character.PARAGRAPH_SEPARATOR: // Zp
            case Character.SPACE_SEPARATOR: // Zs
                return false;
            default:
                return true;
        }
    }
    
    private static String codePointToString(int codePoint) {
        int a[] = {codePoint};
        return new String(a, 0, a.length);
    }
    
    
    private static void indent(int levels) {
        if (levels > 0)
            System.out.print(new String(new char[levels * 2]).replace('\0', ' '));
    }
    
    private static int[] processBitClass(boolean[] bits) {
        assert bits.length <= 256;
        
        int[] arr = new int[bits.length];
        int p = 0;
        
        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                arr[p] = i;
                p++;
            }
        }
        
        return Arrays.copyOf(arr, p);
    }
    
    private static void printCodePoints(int depth, int[] codePoints) {
        switch (loggingLevel) {
            case VERBOSE:
                indent(depth + 1);
                for (int cp: codePoints) {
                    if (isPrintableCodePoint(cp)) {
                        System.out.print(codePointToString(cp));
                    } else {
                        System.out.printf("\\u{%04X}", cp);
                    }
                }
                System.out.println(); 
            case SIMPLE:
                indent(depth + 1);
                for (int cp: codePoints) {
                    System.out.printf("[U+%04X]", cp);
                }
                System.out.println();
        }
    }
    
    // private static String getCtypeName(int type) {
    
    private static final Map<Integer, String> CTYPE_NAME;
    
    static {
        Class asciiClass;
        try {
            asciiClass = Class.forName("java.util.regex.ASCII");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unexpected implementation of java.util.regex package");
        }
        
        Map<Integer, String> m = new HashMap<Integer, String>();
        
        Field[] typeConstants = asciiClass.getDeclaredFields();
        
        try {
            for (Field f: typeConstants) {
                if (Integer.TYPE.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    m.put(f.getInt(asciiClass), f.getName());
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        
        CTYPE_NAME = Collections.unmodifiableMap(m);
    }
    
    private static final String UNKNOWN_DECLARED_FIELD_FORMAT = "DEBUG: Unknown field in anonymous CharProperty: %s\n";
    
    private static void checkUnusedNextNode(Object node) throws IllegalAccessException {
        if (!Node.isAssignableFrom(node.getClass())) {
            throw new IllegalArgumentException("Instance of Pattern$Node or its subclass expected");
        }
        
        Field acceptField = getDeclaredField(Pattern.class, "accept");
        Object accept = acceptField.get(Pattern.class);
        
        Field nextField = getDeclaredField(Node, "next");
        Object nextNode = nextField.get(node);
        
        if (accept != nextNode) {
            throw new InternalError("Pattern.accept expected");
        }
    }
    
    private static void dissectCharProp(int depth, boolean outerCharProp, Object node) throws IllegalAccessException {
        Class<?> charPropClass = patterns.get("CharProperty");
        
        if (!charPropClass.isInstance(node)) {
            throw new InternalError("CharProperty object expected");
        }
        
        Class clazz = node.getClass();
       
        if (clazz.isAnonymousClass()) {
            Class enclosingClass = clazz.getEnclosingClass();
            Method enclosingMethod = clazz.getEnclosingMethod();
                      
            String methodName = enclosingClass.getSimpleName() + "." + enclosingMethod.getName();
            
            switch (methodName) {
                case "CharProperty.complement":
                case "Pattern.union":
                case "Pattern.intersection":
                    info(depth, methodName);
                    
                    for (Field f: clazz.getDeclaredFields()) {
                        if (charPropClass.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            
                            Object innerNode = f.get(node);
                            
                            dissectCharProp(depth + 1, false, innerNode);
                        } else {
                            System.out.printf(UNKNOWN_DECLARED_FIELD_FORMAT, f.getName());
                        }
                    }
                    
                    break;
                case "Pattern.setDifference":
                    info(depth, methodName);
                    
                    Object lhs = getDeclaredField(clazz, "val$lhs").get(node);
                    dissectCharProp(depth + 1, false, lhs);
                    
                    Object rhs = getDeclaredField(clazz, "val$rhs").get(node);
                    dissectCharProp(depth + 1, false, rhs);
                    
                    break;
                case "Pattern.rangeFor":
                    int lower = getDeclaredField(clazz, "val$lower").getInt(node);
                    int upper = getDeclaredField(clazz, "val$upper").getInt(node);
                    
                    info(depth, methodName, lower, upper);

                    break;
                default:
                    System.out.println("DEBUG charProp: " + enclosingMethod);
                    System.out.println("DEBUG charProp: " + enclosingMethod.getName());
                    System.out.println("DEBUG charProp: " + enclosingMethod.getName());
            }
        } else {
            String nodeName = clazz.getSimpleName();
            
            switch (nodeName) {
                case "Ctype":
                    int ctype = getDeclaredField(node.getClass(), "ctype").getInt(node);
                    
                    info(depth, nodeName, CTYPE_NAME.get(ctype));
                    break;
                case "BitClass":  
                    boolean[] bits = (boolean[]) getDeclaredField(node.getClass(), "bits").get(node);
                    int codePoints[] = processBitClass(bits);
                    
                    info(depth, nodeName, codePoints.length);
                    printCodePoints(depth, codePoints);
                    break;
                    case "SingleS":
                case "Single":
                    Class singleClass = patterns.get(nodeName);
                    Field codePointField = getDeclaredField(singleClass, "c");
                    
                    int codePoint = codePointField.getInt(node);
                    
                    info(depth, nodeName, codePoint, Character.getName(codePoint));
                    break;
                case "SingleU":
                    Class singleUClass = patterns.get(nodeName);
                    Field lowerField = getDeclaredField(singleUClass, "lower");
                    
                    /*
                     * The character passed to SingleU has gone through toLowerCase(toUpperCase())
                     * and the character being tested in isSatisfiedBy() also go through the same 
                     * chain of conversion. This is necessary to make sure cases such as SIGMA
                     * character in Greek is correct (there are 2 forms of lower case SIGMA).
                     * 
                     *     System.out.println("\u03c3".matches("(?ui)\u03c2"));
                     *     System.out.println("\u03c2".matches("(?ui)\u03c3"));
                     */
                    
                    /* 
                     * Java Pattern class is in accordance to Unicode Regular Expression (UTS #18),
                     * since it supports simple (1:1 case folding), case-insensitive matching.
                     */
                    int lowercaseCodePoint = lowerField.getInt(node);
                    info(depth, nodeName, lowercaseCodePoint, Character.getName(lowercaseCodePoint));
                    break;
                default:
                    indent(depth);
                    System.out.println("DEBUG charProp: " + clazz.getName());
            }
        }
        
        if (!outerCharProp) {
            checkUnusedNextNode(node);
        }
        
        // System.out.println(Arrays.toString(currNode.getClass().getDeclaredFields()));
    }
    
    static class Work {
        int depth;
        Object node;
        Object branchConn;
        
        @SuppressWarnings("unchecked")
        Work(int depth, Object node, Object branchConn) {
            this.depth = depth;
            
            if (!Node.isAssignableFrom(node.getClass())) {
                throw new IllegalArgumentException("Object of Pattern.Node class or subclass expected");
            }
            this.node = node;
            
            if (branchConn != null && !patterns.get("BranchConn").isAssignableFrom(branchConn.getClass())) {
                throw new IllegalArgumentException("Object of Pattern.BranchConnection class expected");
            }
            this.branchConn = branchConn;
        }
        
        Work(int depth, Object node) {
            this(depth, node, null);
        }
    }
    
    private static void dissectNode(int initDepth, Object rootNode) throws IllegalAccessException { 
        Field nextField = getDeclaredField(Node, "next");
        
        Stack<Work> workQueue = new Stack<Work>();
        workQueue.push(new Work(initDepth, rootNode));
        
        while (!workQueue.isEmpty()) {
            Work work = workQueue.pop();
            
            int depth = work.depth;
            Object node = work.node;
            Object branchConn = work.branchConn;
            
            Object nextNode = nextField.get(node);
            if (nextNode != null) {
                workQueue.push(new Work(depth, nextNode, branchConn));
            }
            
            Class clazz = node.getClass();
            String nodeName = clazz.getSimpleName();
            
            switch (nodeName) {
                case "Start":
                case "StartS":
                    Class startClass = patterns.get("Start");
                    Field minLengthField = getDeclaredField(startClass, "minLength");
                    
                    int minLength = minLengthField.getInt(node);
                        
                    info(depth, nodeName, minLength);
                    break;
                case "Caret":
                case "UnixCaret":
                case "Begin":
                case "End":
                case "LastMatch":
                    info(depth, nodeName);
                    break;
                case "Dollar":
                case "UnixDollar":
                    Class dollarClass = patterns.get(nodeName);
                    Field multilineField = getDeclaredField(dollarClass, "multiline");
                    
                    boolean multiline = multilineField.getBoolean(node);
                    
                    info(depth, nodeName + "(multiline=" + multiline + ")");
                    break;
                case "Node":
                    /**
                     * By default, Node will just accept the match.
                     * 
                     * getSimpleName returns empty string for anonymous class, so we don't have
                     * to worry about misidentification.
                     */
                    
                    info(depth, nodeName);
                    break;
                
                case "Slice":
                case "SliceS":
                    Class sliceNodeClass = patterns.get("SliceNode");
                    Field sliceBufferField = getDeclaredField(sliceNodeClass, "buffer");
                    
                    int[] sliceSequence = (int[]) sliceBufferField.get(node);
                    
                    info(depth, nodeName);
                    printCodePoints(depth, sliceSequence);
                    break;
                case "BnMS":
                case "BnM":
                    int[] bnmSequence = (int[]) getDeclaredField(patterns.get("BnM"), "buffer").get(node);
                    
                    /*
                     * Due to a bug in BnM class static optimize() function, BnMS class will
                     * never be created.
                     * 
                     * Will be fixed in Java 9: https://bugs.openjdk.java.net/browse/JDK-8035076
                     */
                    if (nodeName.equals("BnMS")) {
                        int lengthInChars = getDeclaredField(patterns.get("BnMS"), "lengthInChars").getInt(node);
                        
                        info(depth, nodeName, bnmSequence.length, lengthInChars);
                    } else {
                        info(depth, nodeName, bnmSequence.length);
                    }
                    
                    printCodePoints(depth, bnmSequence);
                    
                    break;
                case "Branch":
                    Class branchClass = patterns.get(nodeName);
                    
                    Object[] nodes = (Object[]) getDeclaredField(branchClass, "atoms").get(node);
                    int size = getDeclaredField(branchClass, "size").getInt(node);
                    Object conn = getDeclaredField(branchClass, "conn").get(node);
                    
                    printObjectTree(conn);
                    
                    info(depth, nodeName, size);
                    
                    System.out.println("nxt" + nextNode.getClass() + " " + nextNode);
                    workQueue.push(new Work(depth + 1, conn, branchConn));
                    
                    for (int i = size - 1; i >= 0; i--) {
                        workQueue.push(new Work(depth + 1, nodes[i], conn));
                    }
                    
                    break;
                case "BranchConn":
                    /**
                     * Copied from source code:
                     * 
                     *   A Guard node at the end of each atom node in a Branch. It
                     *   serves the purpose of chaining the "match" operation to
                     *   "next" but not the "study", so we can collect the TreeInfo
                     *   of each atom node without including the TreeInfo of the
                     *   "next".
                     * 
                     * In short, this node is necessary for collecting information, rather
                     * than matching purpose.
                     */
                    
                    if (node == branchConn) {
                        // Prevent duplicate printing, since BranchConn connects to the next node
                        workQueue.pop(); 
                        
                        indent(depth);
                        System.out.println("---");
                    } else {
                        info(depth, nodeName); 
                    }
                    
                    break;
                case "Behind":
                    Class behindClass = patterns.get(nodeName);
                    Object cond = getDeclaredField(behindClass, "cond").get(node);
                    
                    workQueue.push(new Work(depth + 1, cond));
                    break;
                default:
                    if (patterns.get("CharProperty").isInstance(node)) {
                        dissectCharProp(depth, true, node);
                        // System.out.println(nodeName);
                        // System.out.println(node.getClass());
                    } else {
                        indent(depth);
                        System.out.println("DEBUG node: " + clazz.getName());
                    }
            }
        }
    }
    
    private static Method printObjectTreeMethod = getDeclaredMethod(Pattern.class, "printObjectTree", Node);
    
    private static void printObjectTree(Object node) {
        try {
            printObjectTreeMethod.invoke(Pattern.class, node);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void dissect(Pattern pattern) throws IllegalAccessException {
        Object rootNode = getDeclaredField(Pattern.class, "root").get(pattern);

        System.out.println(pattern.toString());
        
        if ((pattern.flags() & Pattern.CANON_EQ) != 0) {
            String normalizedPattern = (String) getDeclaredField(Pattern.class, "normalizedPattern").get(pattern);
            System.out.println(normalizedPattern);
        }
        
        // printObjectTree(rootNode);
        dissectNode(0, rootNode);
        
        System.out.println();
    }
    
    public static void dissect(String pattern) throws IllegalAccessException {
        dissect(Pattern.compile(pattern));
    }
    
    /**
     * This method is recommended over dissect(java.util.regex.Pattern) method when CANON_EQ flag is specified.
     * 
     * CANON_EQ mode is far from usable, and there are quite a number of bugs:
     * - https://bugs.openjdk.java.net/browse/JDK-4867170
     * - https://bugs.openjdk.java.net/browse/JDK-7080302
     */
    public static void dissect(String pattern, int flags) throws IllegalAccessException {
        
    }
    
    private static void testAnchors() throws IllegalAccessException {
        dissect("^abc$");
        dissect("(?d)^abc$");
        dissect("(?m)^abc$");
        dissect("(?dm)^abc$");
        dissect("\\Aabc\\Z");
        dissect("\\Aabc\\z");
        dissect("\\Gabc");
    }
    
    private static void testSequence() throws IllegalAccessException {
        dissect("abc");
        
        dissect("\\011");
        dissect("\\011\\x3f");
        
        dissect("\uFEFF");
        dissect("\uD83C\uDC04");
        dissect("\\uD83C\\uDC04");
        dissect("\\uD83C\uDC04");

        dissect("(?i)\u03c2");
        dissect("(?iu)\u03c2");
        
        dissect("kalsdklkasjd");
        dissect("\\QNikropoeka*Syn34@ma{re}lgin'Skper?\\E");
        dissect("\\uD83C\uDC04&%\uD83C\\uDC04\\uD83C\\uDC04\uD83C\uDC04Nik\\[rk\\}");
        dissect("abf\uD83C\uDC04\uD83C\uDC05\uD83C\uDC06\uD83C\uDC04\uD83C\uDC04abfgh\uD83C\uDC06\uD83C\uDC04\uD83C\uDC04\uD83C\uDC04\uD83C\uDC04");
    }
    
    private static void testShorthandPOSIXCharacterClass() throws IllegalAccessException {
        dissect("\\d");
        dissect("\\w");
        dissect("\\s");
        dissect("\\D");
        dissect("\\W");
        dissect("\\S");
        
        dissect("\\p{ASCII}");
        dissect("\\p{Alnum}");
        dissect("\\p{Alpha}");
        dissect("\\p{Blank}");
        dissect("\\p{Cntrl}");
        dissect("\\p{Digit}");
        dissect("\\p{Graph}");
        dissect("\\p{Lower}");
        dissect("\\p{Print}");
        dissect("\\p{Punct}");
        dissect("\\p{Space}");
        dissect("\\p{Upper}");
        dissect("\\p{XDigit}");
    }
    
    private static void testNegateAndUnionCharClass() throws IllegalAccessException {
        dissect("[^0-9]");
        dissect("[^[^0-9]]");
        dissect("[^[^[^0-9]]]");
        
        dissect("[[^0-9]2]");
        dissect("[\\D2]");
        
        dissect("[013-9]");
        dissect("[^\\D2]");
        dissect("[^2\\D]");
        dissect("[^[^0-9]2]");
        dissect("[^[^[^0-9]]2]");
        dissect("[^[^[^[^0-9]]]2]");
        dissect("[^2[^0-9]]");
        dissect("[^2[^[^0-9]]]");
        
        dissect("[^[45][^67]]");
        dissect("[^45[^67]]");
        dissect("[^0459[^67]]");
        dissect("[^45[^67\\D]]");
        
        dissect("[^3[^2]]");
        dissect("[^3[^25-6]]");
        
        dissect("[a-z&&[^bc]]");
        dissect("[^\\w\\d\\s]");
    }
    
    private static void testFlatCharacterClass() throws IllegalAccessException {
        
        dissect("[^^a]");
        dissect("[^a&^g]");
        dissect("[^^a&&^g]");
        
        dissect("[a-z\\p{Digit}\\w\u014d\u00c0\u00c1\u00c2\u014d]");
        
        dissect("[a&&^g&&\\pL]");
        dissect("[^^a&&^g&&\\pL]");
        
        dissect("[&&abc]");
        dissect("[abcde&&efghij]");
        dissect("[^abcde&&efghij]");
        
        dissect("[\\p{Alpha}a-c103&&\\p{Alnum}g-p89&&\\p{Digit}A-R]");
        dissect("[\\p{Alpha}a-c103&&^\\p{Alnum}g-p89&&\\p{Digit}A-R]");
        dissect("[^\\p{Alpha}a-c103&&^\\p{Alnum}g-p89&&\\p{Digit}A-R]");
        
        dissect("[^[abc-f]gh&&[a-d]]");
        dissect("[^[abc-f]gh&&[a-d]123p-z]");
        
        dissect("[0-9[a-z]A-Z]");
        dissect("[0-9&&[a-z]A-Z]");
        dissect("[0-9&&a-z[A-Z]]");
        dissect("[0-9&&a-zA-Z]");
        dissect("[0-9a-z&&[01234-6][7-9abc]]");
    }
    
    private static void testJDK_8032926() throws IllegalAccessException {
        dissect(Pattern.compile("\u00fc", Pattern.CANON_EQ));
        dissect(Pattern.compile("\\Q\u00fc\\E", Pattern.CANON_EQ));
    }
    
    private static void testAlternation() throws IllegalAccessException {
        dissect("(12|23|34|45)");
        dissect("^(?:12|23|34|45)$");
        
        dissect("^0(?:12|23|34|45)0$");
        
        dissect("^0(abc|de(g|h)f)0$");
        dissect("^12|23|34|45$");
    }
    
    public static void main(String args[]) {
        try {
            // testAnchors();
            // testSequence();
            // testShorthandPOSIXCharacterClass();
            testNegateAndUnionCharClass();
            dissect("[\\a-\\f]");
            dissect("[a[^b[c[^d]e]f]g]");
            // dissect("[f]j");
            // dissect("[^\\\\\"]");
            // testAlternation();
            // testFlatCharacterClass();
            
            /*
            dissect(Pattern.compile("\u2ADC", Pattern.CANON_EQ));
            dissect("(?c)\u2ADC");
            dissect("(?c)\\u2ADC");
            dissect(Pattern.compile("\u212B", Pattern.CANON_EQ));
            dissect(Pattern.compile("\u0344", Pattern.CANON_EQ));
            dissect(Pattern.compile("\u1ebf", Pattern.CANON_EQ));
            dissect(Pattern.compile("\uac00", Pattern.CANON_EQ));
            
            dissect(Pattern.compile("(?<=a)"));
            
            testJDK_8032926();
            // dissect(Pattern.compile("[\u1f80\u1f82]", Pattern.CANON_EQ));
            */
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
        
        // System.out.println("\ud856");
        // System.out.println("\udf56");
        
        // Try out CANON_EQ and Unicode case insensitive on backreference.
    }
}