package experimentalcode.shared.evaluation.classifier;

import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import experimentalcode.shared.algorithm.classifier.Classifier;
import experimentalcode.shared.evaluation.classifier.procedure.EvaluationProcedure;

/**
 * Provides the prediction performance measures for a classifier
 * based on the confusion matrix.
 *
 * @author Arthur Zimek
 */
public class ConfusionMatrixBasedEvaluation<O, L extends ClassLabel, C extends Classifier<O, L>> extends AbstractClassifierEvaluation<O, L, C> {
    /**
     * Holds the confusion matrix.
     */
    private ConfusionMatrix confusionmatrix;

    /**
     * Holds the used EvaluationProcedure.
     */
    private EvaluationProcedure<O, L, C> evaluationProcedure;

    /**
     * Provides an evaluation based on the given confusion matrix.
     *
     * @param confusionmatrix     the confusion matrix to provide
     *                            the prediction performance measures for
     * @param classifier          the classifier this evaluation is based on
     * @param rep            the training set this evaluation is based on
     * @param testset             the test set this evaluation is based on
     * @param evaluationProcedure the evaluation procedure used
     */
    public ConfusionMatrixBasedEvaluation(ConfusionMatrix confusionmatrix, C classifier, Database database, Database testset, EvaluationProcedure<O, L, C> evaluationProcedure) {
        super(database, testset, classifier);
        this.confusionmatrix = confusionmatrix;
        this.evaluationProcedure = evaluationProcedure;
    }

    @Override
    public void outputEvaluationResult(PrintStream out) {
        out.println("\nEvaluation:");
        out.println(evaluationProcedure.getClass().getName());
        //out.println(evaluationProcedure.setting());
        out.print("\nAccuracy: \n  correctly classified instances: ");
        out.println(confusionmatrix.truePositives());
        out.print("true positive rate:         ");
        double tpr = confusionmatrix.truePositiveRate();
        out.println(tpr);
        out.print("false positive rate:        ");
        out.println(confusionmatrix.falsePositiveRate());
        out.print("positive predicted value:   ");
        double ppv = confusionmatrix.positivePredictedValue();
        out.println(ppv);
        out.print("F1-measure:                 ");
        out.println((2 * ppv * tpr) / (ppv + tpr));
        out.println("\nconfusion matrix:\n");
        out.println(confusionmatrix.toString());
    }


    @Override
    public String getLongName() {
      return "confusionmatrixresult";
    }

    @Override
    public String getShortName() {
      return "confusionmatrixresult";
    }
}
