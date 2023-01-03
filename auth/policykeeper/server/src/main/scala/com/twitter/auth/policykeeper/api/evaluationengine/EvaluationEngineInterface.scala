package com.twitter.auth.policykeeper.api.evaluationengine

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future

final case class InvalidRuleExpression() extends Exception

trait EvaluationEngineInterface[T <: Expression] extends PerPolicyMetrics {
  protected val expressionParser: ExpressionParserInterface[T]
  protected val expressionEvaluator: ExpressionEvaluatorInterface[T]
  protected val statsReceiver: StatsReceiver
  protected val logger: JsonLogger

  private[evaluationengine] val PolicyRequested = "policyRequested"
  private[evaluationengine] val PolicyFailedDueToIncompleteInput = "policyFailedIncompleteInput"
  private[evaluationengine] val PolicyFailedDueToInvalidRuleExpression =
    "policyFailedInvalidRuleExpression"
  private[evaluationengine] val PolicyFailedDueToInputCastException =
    "policyFailedDueToInputCastException"
  private[evaluationengine] val PolicyFailedDueToOtherException =
    "policyFailedDueToOtherException"
  private[evaluationengine] val ParsingTime = "parsingTime"
  private[evaluationengine] val EvaluationTime = "evaluationTime"

  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)
  private[evaluationengine] val policyRequestedCounter = statsScope.counter(PolicyRequested)
  private[evaluationengine] val policyFailedDueToIncompleteInputCounter =
    statsScope.counter(PolicyFailedDueToIncompleteInput)
  private[evaluationengine] val policyFailedDueToInvalidRuleExpressionCounter =
    statsScope.counter(PolicyFailedDueToInvalidRuleExpression)
  private[evaluationengine] val policyFailedDueToInputCastExceptionCounter =
    statsScope.counter(PolicyFailedDueToInputCastException)
  private[evaluationengine] val policyFailedDueToOtherExceptionCounter =
    statsScope.counter(PolicyFailedDueToOtherException)

  private[evaluationengine] val expressionParsingTime = statsScope.stat(ParsingTime)
  private[evaluationengine] val expressionEvaluationTime = statsScope.stat(EvaluationTime)

  /**
   * Validates a policy. Checks if policy has valid set of expressions and all required input values are presented
   *
   * @param policy
   * @param input
   * @return
   */
  def validatePolicyWithInput(
    policy: Policy,
    input: ExpressionInput
  ): Future[Boolean] = {
    Future
      .collect(
        policy.rules.map { rule =>
          val expression = expressionParser.parse(rule.expression)
          expression.isValid match {
            case true if expression.requiredInput().subsetOf(input.keySet) => Future.True
            case _ => Future.False
          }
        }.toSeq
      ).map(results => !results.contains(false))
  }

  private def policyNeverFail(policy: Policy): Boolean = {
    policy.rules.exists(_.fallbackAction.isDefined)
  }

  /**
   * Executes a policy. Returns rule action of the most prioritized rule (0 is the highest priority)
   * which is evaluated to true
   *
   * @param policy
   * @param input
   * @return
   * @throws InvalidRuleExpression
   * @throws IncompleteInputException
   */
  def execute(
    policy: Policy,
    input: ExpressionInput
  ): Future[Option[RuleAction]] = {
    //TODO (AUTHPLT-2239): adjust method output
    val policyCanFail = !policyNeverFail(policy = policy)
    policyStatsScope(policy).counter(PolicyRequested).incr()
    policyRequestedCounter.incr()
    aggregateExpressionResults(
      Future.collect(
        policy.rules.map { rule =>
          val expression = Stat.time(policyStatsScope(policy).stat(ParsingTime)) {
            Stat.time(expressionParsingTime) {
              expressionParser.parse(rule.expression)
            }
          }
          (
            expression.isValid match {
              case true if expression.requiredInput().subsetOf(input.keySet) =>
                Stat.timeFuture(policyStatsScope(policy).stat(EvaluationTime)) {
                  Stat.timeFuture(expressionEvaluationTime) {
                    expressionEvaluator
                      .evaluateExpression(
                        expression = expression,
                        expressionInput = input
                      ).map { result =>
                        ExpressionResult(rule.priority, result, rule.action)
                      }
                  }
                }
              case true =>
                Future.exception(IncompleteInputException())
              case _ =>
                Future.exception(InvalidRuleExpression())
            }
          ).rescue {
            case typeCastE: ExpressionInputTypeCastException =>
              failOrFallback(
                policyCanFail = policyCanFail,
                rule = rule,
                () => {
                  policyFailedDueToInputCastExceptionCounter.incr()
                  policyStatsScope(policy)
                    .counter(PolicyFailedDueToInputCastException).incr()
                  Future.exception(typeCastE)
                }
              )
            case incompleteInputException: IncompleteInputException =>
              failOrFallback(
                policyCanFail = policyCanFail,
                rule = rule,
                () => {
                  policyFailedDueToIncompleteInputCounter.incr()
                  policyStatsScope(policy).counter(PolicyFailedDueToIncompleteInput).incr()
                  loggerScope.warning(
                    message = "policy failed due to incomplete input",
                    metadata = Some(
                      Map(
                        "policyId" -> policy.policyId,
                        "ruleExpression" -> rule.expression,
                        "input" -> input.map { case (k, _) => k.toString() }))
                  )
                  Future.exception(incompleteInputException)
                }
              )
            case invalidRuleExpression: InvalidRuleExpression =>
              failOrFallback(
                policyCanFail = policyCanFail,
                rule = rule,
                () => {
                  policyFailedDueToInvalidRuleExpressionCounter.incr()
                  policyStatsScope(policy).counter(PolicyFailedDueToInvalidRuleExpression).incr()
                  loggerScope.warning(
                    message = "policy failed due to invalid rule expression",
                    metadata =
                      Some(Map("policyId" -> policy.policyId, "ruleExpression" -> rule.expression))
                  )
                  Future.exception(invalidRuleExpression)
                }
              )
            case e =>
              failOrFallback(
                policyCanFail = policyCanFail,
                rule = rule,
                () => {
                  policyFailedDueToOtherExceptionCounter.incr()
                  policyStatsScope(policy).counter(PolicyFailedDueToOtherException).incr()
                  loggerScope.error(
                    message = "policy failed due other exception",
                    metadata = Some(
                      Map(
                        "policyId" -> policy.policyId,
                        "ruleExpression" -> rule.expression,
                        "exception" -> e.getMessage))
                  )
                  Future.exception(e)
                }
              )
          }
        }.toSeq
      )
    )
  }

  /**
   * Merges rule evaluation results and returns action of the most prioritized rule (0 is the highest priority)
   * which is evaluated to true
   *
   * @param expressionResults future boolean expressions to merge
   * @return
   */
  private def aggregateExpressionResults(
    expressionResults: Future[Seq[ExpressionResult]]
  ): Future[Option[RuleAction]] = expressionResults.map { expressionResults =>
    val prioritizer = ExpressionResultPrioritizer()
    prioritizer.mergeResults(expressionResults)
    prioritizer.top() match {
      case None => None
      case Some(p) => Some(p.requestedAction)
    }
  }

  /**
   * If [[rule]] has a fallback action returns ExpressionResult based on it
   * if policy doesn't contain any rules with fallback actions executes the [[onFailure]] callback.
   * otherwise returns false result without action
   *
   * Attention! Fallback action is considered as expected behavior therefore
   * logs, metrics and exceptions from [[onFailure]] callback will not be initiated
   * with specified fallbackAction property
   *
   * @param rule
   * @param onFailure
   * @return
   */
  private def failOrFallback(
    policyCanFail: Boolean,
    rule: Rule,
    onFailure: () => Future[ExpressionResult]
  ): Future[ExpressionResult] = {
    rule.fallbackAction match {
      case Some(action) =>
        Future.value(
          ExpressionResult(rule.priority, evaluationResult = true, requestedAction = action))
      case None if policyCanFail => onFailure()
      case None =>
        Future(
          ExpressionResult(
            rule.priority,
            evaluationResult = false,
            requestedAction = RuleAction(actionNeeded = false)))
    }
  }

}
