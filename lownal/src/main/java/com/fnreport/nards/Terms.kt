
package com.fnreport.nards

import com.fnreport.*
import com.fnreport.TokenMatcher.Companion.nd_
import com.fnreport.nards.carrion.*


operator fun String.plus(re: WithRegex) = this + re.regex
/**confix*/
operator fun String.plus(re: Regex) = "()" / this + "()" / re.pattern

operator fun String.div(re: String) = this[0] + re + this[1]


enum class carrion (rel:rel): RegexEmitter by rel{
    period(rel("\\.")  ),
    ws(  rel("\\s*") )     ,
    capture(rel(".*") ),
}


/*

task ::= [budget] sentence                       (* task to be processed *)

sentence ::= statement"." [tense] [truth]            (* judgement to be absorbed into beliefs *)
| statement"?"            [tense] [truth]            (* question on thuth-value to be answered *)
| statement"!"            [desire]                   (* goal to be realized by operations *)
| statement"@"            [desire]                   (* question on desire-value to be answered *)

statement ::= <"<">term copula term<">">              (* two terms related to each other *)
| <"(">term copula term<")">              (* two terms related to each other, new notation *)
| term                                    (* a term can name a statement *)
| "(^"word {","term} ")"                  (* an operation to be executed *)
| word"("term {","term} ")"               (* an operation to be executed, new notation *)

term ::= word                                    (* an atomic constant term *)
| variable                                (* an atomic variable term *)
| compound-term                           (* a term with internal structure *)
| statement                               (* a statement can serve as a term *)

desire ::= truth                                   (* same format, different interpretations *)
truth ::= <"%">frequency[<";">confidence]<"%">    (* two numbers in [0,1]x(0,1) *)
budget ::= <"$">priority[<";">durability][<";">quality]<"$"> (* three numbers in [0,1]x(0,1)x[0,1] *)

compound-term ::= op-ext-set term {"," term} "}"          (* extensional set *)
| op-int-set term {"," term} "]"          (* intensional set *)
| "("op-multi"," term {"," term} ")"      (* with prefix operator *)
| "("op-single"," term "," term ")"       (* with prefix operator *)
| "(" term {op-multi term} ")"            (* with infix operator *)
| "(" term op-single term ")"             (* with infix operator *)
| "(" term {","term} ")"                  (* product, new notation *)
| "(" op-ext-image "," term {"," term} ")"(* special case, extensional image *)
| "(" op-int-image "," term {"," term} ")"(* special case, \ intensional image *)
| "(" op-negation "," term ")"            (* negation *)
| op-negation term                        (* negation, new notation *)

variable ::= "$"word                                 (* independent variable *)
| "#"word                                 (* dependent variable *)
| "?"word                                 (* query variable in question *)

copula ::= "-->"                                   (* inheritance *)
| "<->"                                   (* similarity *)
| "{--"                                   (* instance *)
| "--]"                                   (* property *)
| "{-]"                                   (* instance-property *)
| "==>"                                   (* implication *)
| "=/>"                                   (* predictive implication *)
| "=|>"                                   (* concurrent implication *)
| "=\\>"                                  (* =\> retrospective implication *)
| "<=>"                                   (* equivalence *)
| "</>"                                   (* predictive equivalence *)
| "<|>"                                   (* concurrent equivalence *)

op-int-set::= "["                                     (* intensional set *)
op-ext-set::= "{"                                     (* extensional set *)
op-negation::= "--"                                    (* negation *)
op-int-image::= "\\"                                    (* \ intensional image *)
op-ext-image::= "/"                                     (* extensional image *)
op-multi ::= "&&"                                    (* conjunction *)
| "*"                                     (* product *)
| "||"                                    (* disjunction *)
| "&|"                                    (* parallel events *)
| "&/"                                    (* sequential events *)
| "|"                                     (* intensional intersection *)
| "&"                                     (* extensional intersection *)
op-single ::= "-"                                     (* extensional difference *)
| "~"                                     (* intensional difference *)

tense ::= ":/:"                                   (* future event *)
| ":|:"                                   (* present event *)
| ":\\:"                                  (* :\: past event *)


word : #"[^\ ]+"                               (* unicode string *)
priority : #"([0]?\.[0-9]+|1\.[0]*|1|0)"           (* 0 <= x <= 1 *)
durability : #"[0]?\.[0]*[1-9]{1}[0-9]*"             (* 0 <  x <  1 *)
quality : #"([0]?\.[0-9]+|1\.[0]*|1|0)"           (* 0 <= x <= 1 *)
frequency : #"([0]?\.[0-9]+|1\.[0]*|1|0)"           (* 0 <= x <= 1 *)
confidence : #"[0]?\.[0]*[1-9]{1}[0-9]*"             (* 0 <  x <  1 *)
*/


enum class accounting(sym:String, override var symbol:String=sym, var rel: rel = rel(sym)) : RegexEmitter by rel {
    /** same format, different interpretations */
    desire("%%" / ("(" + fragment.frequency.regex + ")(;" + fragment.confidence.regex + ")?")),
    /** two numbers in [0,1]x(0,1) */
    truth(
            desire.input
    ),
    /** three numbers in [0,1]x(0,1)x[0,1] */
    budget(
            "$$" / (fragment.priority.regex.pattern + "(;" + fragment.durability + ")?(;" + fragment.quality + ")?")
    );
}

enum class variable(override var input: String) : WithRegex {
    /** independent variable */
    independent_variable("$" + fragment.word),
    /** dependent variable */
    dependent_variable("#" + fragment.word),
    /** query variable in question */
    query_variable_in_question("?" + fragment.word),
}

enum class tokenizer(override var input: String) : WithRegex { ws("\\s+"), }

private val fractionalpart = "([01]([.]\\d*)?|[.]\\d{1,})"

enum class fragment(override val input: String) : WithRegex {

    /** unicode string */
    word("[^\\s]+"),
    /** 0 <= x <= 1 */
    priority( fractionalpart ),
    /** 0 <  x <  1 */
    durability( fractionalpart ),
    /** 0 <= x <= 1 */
    quality( fractionalpart ),
    /** 0 <= x <= 1 */
    frequency(fractionalpart),
    /** 0 <  x <  1 */
    confidence(fractionalpart),

}


enum class copula(sym:String, override var symbol:String=sym, var lit: lit = lit(sym)) : SymbolWithInput by lit {
    /*** inheritance*/
    inheritance("-->", "→"),
    /*** similarity*/
    similarity("<->", "↔"),
    /*** instance*/
    instance("{--", "◦→"),
    /*** property*/
    narsproperty("--]", "→◦"),
    /*** instance-property*/
    instance_property("{-]", "◦→◦"),
    /*** implication*/
    implication("==>", "⇒"),
    /*** predictive implication*/
    predictive_implication("=/>", "/⇒"),
    /*** concurrent implication*/
    concurrent_implication("=|>", "|⇒"),
    /*** retrospective implication*/
    retrospective_implication("=\\>", "\\⇒"),
    /*** equivalence*/
    equivalence("<=>", "⇔"),
    /*** predictive equivalence*/
    predictive_equivalence("</>", "/⇔"),
    /*** concurrent equivalence*/
    concurrent_equivalence("<|>", "|⇔"),
    ;

}

enum class term_set(op: String, cl: String, override val opn: OpaqueRegex = OpaqueRegex(op), override val cls: OpaqueRegex = OpaqueRegex(cl)) : SetOp {
    intensional_set("[", "]"),
    extensional_set("{", "}"),;

}

enum class term_connector(sym:String, override var symbol: String =sym, var lit: lit = lit(sym)) : SymbolWithInput by lit {


    negation("--", "¬"),
    intensional_image("\\"),
    extensional_image("/")
}

/** conjunction */
enum class op_multi(sym:String, override var symbol: String =sym, var lit: lit = lit(sym)) : SymbolWithInput by lit {

    //(symbol: String, rep:String=symbol,override val regex: Regex = symbol.map { it }.joinToString(separator = "\\", prefix = "\\").toRegex()) : RegexTokenTest {
    conjunction("&&", "∧"),
    /**product*/
    product("*", "×"),
    /**disjunction*/
    disjunction("||", "∨"),
    /**parallel events*/
    parallel_events("&|", ";"),
    /**sequential events*/
    sequential_events("&/", ","),
    /**intensional intersection*/
    intensional_intersection("|", "∪"),
    /**extensional intersection*/
    extensional_intersection("&", "∩"),
    /**placeholder?*/
    image("`Ω`", "◇")
}

/**op-single*/
enum class op_single(sym:String, override var symbol: String =sym, var lit: lit = lit(sym)) : SymbolWithInput by lit {

    /**`extensional difference`*/
    extensional_difference("-", "−"),
    /**`intensional difference`*/
    intensional_difference("~", "⦵"),
}


enum class tense(sym:String, override var symbol: String =sym, var lit: lit = lit(sym)) : SymbolWithInput by lit {

    /** future event */
    future_event(":/:", "/⇒"),
    /** present event */
    present_event(":|:", "|⇒"),
    /** :\: past event */
    past_event(":\\:", "\\⇒"),
}



/**
sentence ::= statement"." [tense] [truth]            (* judgement to be absorbed into beliefs *)
| statement"?" [tense] [truth]            (* question on thuth-value to be answered *)
| statement"!" [desire]                   (* goal to be realized by operations *)
| statement"@" [desire]                   (* question on desire-value to be answered *)
 */


enum class sentence( var rel: WithRegex) : WithRegex by rel {
    judgement( capture `&` period `&` oneOf(*tense.values()) `?` (ws `&` accounting.truth) `?` nd_    ),
    valuation(capture `&` "\\?" `&` oneOf(*tense.values()) `?` (ws `&` accounting.truth) `?` nd_),
    goal(capture `&` "\\!" `&` (ws `&` accounting.desire) `?` nd_),
    interest(capture `&` "\\@" `&` (ws `&` accounting.desire) `?` nd_) {
        override val symbol get() = "¿"
    },
    ;

}
