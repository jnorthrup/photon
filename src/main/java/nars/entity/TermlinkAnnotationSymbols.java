package nars.entity;

enum TermlinkAnnotationSymbols {
    TO_COMPONENT_1(" @("),
    TO_COMPONENT_2(")_ "),
    TO_COMPOUND_1(" _@("),
    TO_COMPOUND_2(") "),
    ;

    public final String sym;

    TermlinkAnnotationSymbols(String sym) {

        this.sym = sym;
    }
}
