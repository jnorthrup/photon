package com.fnreport.nards


import com.fnreport.nards.accounting.desire
import com.fnreport.nards.accounting.truth
import com.fnreport.nards.statement.operation_to_be_exdecuted
import com.fnreport.nards.statement.related_pair_of_terms
import com.fnreport.nards.statement.statement_name
import com.fnreport.nards.term.comma
import com.fnreport.nards.term.cparen
import com.fnreport.nards.term.oparen
import com.fnreport.nards.term_connector.*
import com.fnreport.nards.term_set.extensional_set
import com.fnreport.nards.term_set.intensional_set
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple

open class Omega

object nd_ : Omega()

val `Ω` = nd_


val period by lazy { lit('.') }
val ws by lazy { regexToken("\\s*".toRegex(), false) }
val capture by lazy { regexToken(".*".toRegex(), false) }
//ws("\\s*"),
//
fun lit(i: Any) = literalToken(i.toString(), i.toString(), false)


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


/**
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
 */


enum class op_multi(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {
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
    image("_", "◇")
}

enum class term_set(pair: Pair<String, String>) {
    intensional_set("[" to "]"),
    extensional_set("{" to "}"), ;

    val first = lit(pair.first)
    val second = lit(pair.second)
}

enum class term_connector(s: Any?, symbol: String? = null, val lit: Token = literalToken(symbol.takeIf { it != null }
        ?: Enum<*>::name as String, s.toString(), false)) : Parser<TokenMatch> by lit {
    negation("--", "¬"),
    intensional_image('\\'),
    extensional_image('/')
}


/**op-single*/
enum class op_single(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {

    /**`extensional difference`*/
    extensional_difference("-", "−"),
    /**`intensional difference`*/
    intensional_difference("~", "⦵"),
}


enum class tense(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {
    /** future event */
    future_event(":/:", "/⇒"),
    /** present event */
    present_event(":|:", "|⇒"),
    /** :\: past event */
    past_event(":\\:", "\\⇒")
    ;
}

object term : Grammar<Any?>() {
    /** conjunction */

    /** conjunction */

    val compoundedTerms by lazy { separatedTerms(term, comma, false) }

    val comma = lit(",")

    val oparen = lit("(")
    val cparen = lit(")")
    val ext_set = -extensional_set.first * compoundedTerms * -extensional_set.second
    val int_set = -intensional_set.first * compoundedTerms * -intensional_set.second
    val prefixed_mutli_value = -oparen * (OrCombinator(op_multi.values().toList())) * -comma * compoundedTerms * -cparen
    val prefixed_single_value = -oparen * (/*OrCombinator(op_multi.values().toList()) or*/ OrCombinator(op_single.values().toList())) * -comma * 2.times(-comma * term) * -cparen
    val infix_multi_value = -oparen * term * (oneOrMore(OrCombinator(op_multi.values().toList())) and term) * -cparen
    val infix_single_value = -oparen * term * OrCombinator(op_single.values().toList()) * term * -cparen
    val special_extensional_image_case = -oparen * term * compoundedTerms * -cparen
    val special_case_intentionsal_image = -oparen * (intensional_image or extensional_image) * -comma * compoundedTerms * -cparen
    val negation1 = -oparen * negation * -comma * term * -cparen
    val negation2 = negation * term
    val compound_term = ext_set or
            int_set or
            prefixed_mutli_value or
            prefixed_single_value or
            infix_multi_value or
            infix_single_value or
            special_extensional_image_case or
            special_case_intentionsal_image or
            negation1 or
            negation2
    /**
    term ::= word                                    (* an atomic constant term *)
    | variable                                (* an atomic variable term *)
    | compound-term                           (* a term with internal structure *)
    | statement                               (* a statement can serve as a term *)
     */
    /**
    term ::= word                                    (* an atomic constant term *)
    | variable                                (* an atomic variable term *)
    | compound-term                           (* a term with internal structure *)
    | statement                               (* a statement can serve as a term *)
     */

    override val rootParser: Parser<Any?> by lazy { word or OrCombinator(variable.values().toList()) or compound_term or (related_pair_of_terms or statement_name or operation_to_be_exdecuted) }

}

object accounting : Grammar<Tuple>() {
    /** same format, different interpretations */
    val desire = -lit('%') and fragment.frequency and optional(-lit(';') and fragment.confidence) and -lit('%')
    /** two numbers in [0,1]x(0,1) */
    val truth = -lit('%') and fragment.frequency and optional(-lit(';') and fragment.confidence) and -lit('%')
    /** three numbers in [0,1]x(0,1)x[0,1] */
    val budget = -lit('$') and fragment.priority and optional(-lit(';') and fragment.durability) and optional(-lit(';') and fragment.quality) and -lit('$')
    override val rootParser =desire or truth or budget

}


val fractionalpart = "([01]([.]\\d*)?|[.]\\d{1,})"
/** unicode string */
val word = regexToken("[^\\s]+") map TokenMatch::text

enum class fragment(s: String) : Parser<Double> by regexToken(s) map { tokenMatch -> tokenMatch.text.toDouble() } {//(s: String) : Parser<TokenMatch> by regexToken(s.toRegex(), false) {

    /** 0 <= x <= 1 */
    priority(fractionalpart),
    /** 0 <  x <  1 */
    durability(fractionalpart),
    /** 0 <= x <= 1 */
    quality(fractionalpart),
    /** 0 <= x <= 1 */
    frequency(fractionalpart),
    /** 0 <  x <  1 */
    confidence(fractionalpart),

}


enum class copula(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {
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


/**
variable ::= "$"word                                 (* independent variable *)
|            "#"word                                 (* dependent variable *)
|            "?"word                                 (* query variable in question *)
 */
enum class variable(s: String, val x: AndCombinator<String> = -lit(s) * word) : Parser<String> by x {
    independent_variable("$"),
    dependent_variable("#"),
    query_variable("?"),
}


/*
(* an atomic constant term *)
| variable                                (* an atomic variable term *)
| compound-term                           (* a term with internal structure *)
| statement                               (* a statement can serve as a term *)
*/

/**
statement ::= <"<">term copula term<">">              (* two terms related to each other *)
| <"(">term copula term<")">                          (* two terms related to each other, new notation *)
| term                                                (* a term can name a statement *)
| "(^"word {","term} ")"                              (* an operation to be executed *)
| word"("term {","term} ")"                           (* an operation to be executed, new notation *)
 */


object statement : Grammar<Any?>() {
    val related_pair_of_terms = (oparen * term * OrCombinator(copula.values().toList()) * term * cparen) or
            (lit("<") * term * OrCombinator(copula.values().toList()) * term * lit(">"))
    val statement_name = term
    val operation_to_be_exdecuted = (-lit("(^") * word * -comma * term * -cparen ) or
            (word * -oparen * term * -comma * term * -cparen)
    override val rootParser by related_pair_of_terms or statement_name or operation_to_be_exdecuted

}

/**
sentence ::= statement"." [tense] [truth]            (* judgement to be absorbed into beliefs *)
| statement"?" [tense] [truth]            (* question on thuth-value to be answered *)
| statement"!" [desire]                   (* goal to be realized by operations *)
| statement"@" [desire]                   (* question on desire-value to be answered *)
 */


object sentence : Grammar<Any?>() {
    val judgement = (capture * -lit('.')  * optional( OrCombinator(tense.values().asList())) * optional(  truth)  )
    val valuation = capture * -lit('?') * optional(OrCombinator(tense.values().asList())) *optional(  truth )
    val goal = (capture  * -lit('!') * optional(  desire )  )
    val interest = (capture * -lit('@') * optional(desire)) //       "¿")
    override val rootParser= judgement or valuation or goal or interest
}
