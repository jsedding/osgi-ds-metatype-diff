package net.distilledcode.tools.osgi;

import java.io.PrintWriter;
import java.util.Stack;

class PrintingVisitor implements Comparison.Visitor {

    private static String INDENTATION_WHITESPACE = "                                                ";

    private boolean hasPrintedSomething;

    private Stack<String> sections = new Stack<>();

    private Stack<String> printedSections = new Stack<>();

    private PrintWriter out;

    public PrintingVisitor(final PrintWriter out) {
        this.out = out;
    }

    @Override
    public void enter(final String sectionName) {
        sections.push(sectionName);
    }

    @Override
    public void leave(final String sectionName) {
        if (sections.size() == printedSections.size()) {
            printedSections.pop();
            if (printedSections.empty() && hasPrintedSomething) {
                out.println();
            }
        }
        sections.pop();
    }

    @Override
    public void added(final String name, final Object value) {
        printSectionHeaderIfNeeded();
        printValue('+', name, value);
    }

    @Override
    public void changed(final String name, final Object leftValue, final Object rightValue) {
        printSectionHeaderIfNeeded();
        removed(name, leftValue);
        added(name, rightValue);
    }

    @Override
    public void removed(final String name, final Object value) {
        printSectionHeaderIfNeeded();
        printValue('-', name, value);
    }

    private void printSectionHeaderIfNeeded() {
        for (int i = printedSections.size(); i < sections.size(); i++) {
            String section = sections.get(i);
            out.append(indent(i)).println(section);
            printedSections.push(section);
            hasPrintedSomething = true;
        }
    }

    private void printValue(final char plusMinus, final String name, final Object value) {
        out.append(indent(sections.size())).append(plusMinus).append(' ').append(name).append(" = ");
        if (value.getClass().isArray()) {
            printArray((Object[]) value);
        } else {
            out.println(value);
        }
    }

    private void printArray(final Object[] values) {
        out.append('[');
        for (int i = 0; i < values.length; i++) {
            if (values.length > 1) {
                out.println();
                out.append(indent(sections.size() + 2));
            }
            out.print(values[i]);
            if (i < values.length - 1) {
                out.append(',');
            }
        }
        if (values.length > 1) {
            out.println();
            out.append(indent(sections.size())).append("  ");
        }
        out.println(']');
    }

    private String indent(final int i) {
        return INDENTATION_WHITESPACE.substring(0, i * 4);
    }

    public boolean hasPrintedSomething() {
        return hasPrintedSomething;
    }
}
