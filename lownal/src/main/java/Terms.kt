package com.fnreport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**confix*/
operator fun String.div(re: String) = this[0] + re + this[1]


operator fun String.plus(re: RegexEmitter) = this + re.regex
operator fun String.plus(re: Regex) = "()" / this + "()" / re.pattern


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


class opt(
    var emitter: RegexEmitter,
    override val name: String = "opt" + ("()" / emitter.name),
    override val regex: Regex = Regex(("()" / emitter.regex.pattern) + "?"),
    override val symbol: String= ("()"/emitter.symbol)+"?"
) : RegexEmitter

enum class accounting(override val symbol: String, vararg decoders: Any) : RegexEmitter {
    /** same format, different interpretations */
    desire(
        "%%" / ("(" + fragment.frequency + ")(;" + fragment.confidence + ")?"),
        opt(fragment.frequency),
        opt(fragment.confidence)
    ),
    /** two numbers in [0,1]x(0,1) */
    truth(desire.symbol, opt(fragment.frequency), opt(fragment.confidence)),
    /** three numbers in [0,1]x(0,1)x[0,1] */
    budget(
        "$$" / (fragment.priority.regex.pattern + "(;" + fragment.durability + ")?(;" + fragment.quality + ")?"),
        fragment.priority,
        opt(fragment.durability),
        opt(fragment.quality)
    );
}

enum class variable(override var symbol: String) : RegexEmitter {
    /** independent variable */
    independent_variable("$" + fragment.word),
    /** dependent variable */
    dependent_variable("#" + fragment.word),
    /** query variable in question */
    query_variable_in_question("?" + fragment.word),
}

enum class tokenizer(override var symbol: String) : RegexEmitter { ws("\\s+"), }

enum class fragment(override val symbol: String) : RegexEmitter {

    /** unicode string */
    word("[^\\ ]+"),
    /** 0 <= x <= 1 */
    priority("[0]?\\.[0-9]+|1\\.[0]*|1|0"),
    /** 0 <  x <  1 */
    durability("[0]?\\.[0]*[1-9]{1}[0-9]*"),
    /** 0 <= x <= 1 */
    quality("[0]?\\.[0-9]+|1\\.[0]*|1|0"),
    /** 0 <= x <= 1 */
    frequency("[0]?\\.[0-9]+|1\\.[0]*|1|0"),
    /** 0 <  x <  1 */
    confidence("[0]?\\.[0]*[1-9]{1}[0-9]*"),

}

interface Emitter {
    val name: String
}

interface RegexEmitter : Emitter {
    val symbol: String
    val rep: String
        get() = symbol
    val regex: Regex
        get() = symbol.toRegex()
}

interface opaqueRegex : RegexEmitter {
    override val regex: Regex
        get() = symbol.map { it }.joinToString(separator = "\\", prefix = "\\").toRegex()

}

interface setOp : opaqueRegex {
    val close: String

}

fun RegexEmitter.test(input: String): Pair<Emitter, MatchGroupCollection>? {
    return regex.find(input)?.let {
        val second: MatchGroupCollection = it.groups
        Pair(this, second)
    } ?: null
}

enum class copula(override val symbol: String, override val rep: String) : opaqueRegex {
    /*** inheritance*/
    inheritance(symbol = "-->", rep = "→"),
    /*** similarity*/
    similarity("<->", "↔"),
    /*** instance*/
    `instance`("{--", "◦→"),
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
}

enum class term_set(override val symbol: String, override var close: String) : setOp {
    intensional_set("[", "]"),
    extensional_set("{", "}"),
}

enum class term_connector(override val symbol: String, override val rep: String = symbol) : opaqueRegex {

    negation("--", rep = "¬"),
    intensional_image("\\"),
    extensional_image("/")
}

/** conjunction */
enum class op_multi(override var symbol: String, override var rep: String = symbol) : opaqueRegex {
    //(symbol: String, rep:String=symbol,override val regex: Regex = symbol.map { it }.joinToString(separator = "\\", prefix = "\\").toRegex()) : RegexEmitter {
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

/**op-single*/
enum class op_single(override val symbol: String, override val rep: String = symbol) : opaqueRegex {
    /**`extensional difference`*/
    extensional_difference("-", "−"),
    /**`intensional difference`*/
    intensional_difference("~", "⦵"),
}


enum class tense(override val symbol: String, override val rep: String = symbol) : opaqueRegex {
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
class sentenceParser {
    fun foo(): Flow<Int> = (1..3).asFlow()

    fun scan(line: String) {
        kotlinx.coroutines.channels.Channel<Sentence>()
        kotlinx.coroutines.channels.Channel<Sentence>()
        kotlinx.coroutines.channels.Channel<Sentence>()
        kotlinx.coroutines.channels.Channel<Sentence>()
    }
}

class oneOf(vararg val testFor: RegexEmitter) : RegexEmitter {
    override val symbol = testFor.map { it.symbol }.joinToString(separator = "|")
    override val regex =
        testFor.map { it.regex.pattern }.joinToString(separator = "|", prefix = "(", postfix = ")").toRegex()
    override val name: String = testFor.map(Emitter::name).joinToString("|")
}

open class Sentence(
    override val name: String,
    override val symbol: String,
    vararg val after: RegexEmitter
) : RegexEmitter {
    override val rep: String
        get() = symbol
    override val regex: Regex
        get() = Regex("([^\\" + symbol + "])+\\" + symbol)
}

val Judgement = Sentence("Judgement", ".", opt(oneOf(*tense.values())), opt(accounting.truth))
val Valuation = Sentence("Valuation","?", opt(oneOf( *tense.values())), opt(accounting.truth))
val Goal =      Sentence("Goal", "!", opt(accounting.desire))
val Interest = object:Sentence("Interest", "@", opt(accounting.desire)) { override val rep = "¿" }

