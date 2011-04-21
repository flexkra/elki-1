package experimentalcode.erich.histogram;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Scale an outlier score using a given scaling function.
 * 
 * @author Erich Schubert
 * 
 * @param <O>
 */
public class NormalizeOutlierScoreMetaAlgorithm<O> extends AbstractAlgorithm<O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(NormalizeOutlierScoreMetaAlgorithm.class);

  /**
   * Association ID for scaled values
   */
  public static final AssociationID<Double> SCALED_SCORE = AssociationID.getOrCreateAssociationID("SCALED_SCORE", Double.class);

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ObjectParameter<Algorithm> ALGORITHM_PARAM = new ObjectParameter<Algorithm>(OptionID.ALGORITHM, OutlierAlgorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  private final ObjectParameter<ScalingFunction> SCALING_PARAM = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);

  /**
   * Holds the algorithm to run.
   */
  private Algorithm algorithm;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Constructor
   * 
   * @param config Parameters
   */
  public NormalizeOutlierScoreMetaAlgorithm(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(ALGORITHM_PARAM)) {
      algorithm = ALGORITHM_PARAM.instantiateClass(config);
    }
    if(config.grab(SCALING_PARAM)) {
      scaling = SCALING_PARAM.instantiateClass(config);
    }
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    OutlierResult or = getOutlierResult(innerresult);
    if(scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) scaling).prepare(database.getDBIDs(), or);
    }

    WritableDataStore<Double> scaledscores = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);

    for(DBID id : database.getDBIDs()) {
      double val = or.getScores().getValueFor(id);
      val = scaling.getScaled(val);
      scaledscores.put(id, val);
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(0.0, 1.0);
    AnnotationResult<Double> scoresult = new AnnotationFromDataStore<Double>("Scaled Outlier", "scaled-outlier", SCALED_SCORE, scaledscores);
    OutlierResult result = new OutlierResult(meta, scoresult);
    result.addChildResult(innerresult);

    return result;
  }

  /**
   * Find an OutlierResult to work with.
   * 
   * @param result Result object
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Result result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors.size() > 0) {
      return ors.get(0);
    }
    throw new IllegalStateException("Comparison algorithm expected at least one outlier result.");
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    try {
      return ((AbstractAlgorithm<?>) algorithm).getInputTypeRestriction();
    }
    catch(ClassCastException e) {
      return TypeUtil.ANY;
    }
  }
}