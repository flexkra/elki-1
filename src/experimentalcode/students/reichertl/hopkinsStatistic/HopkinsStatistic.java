package experimentalcode.students.reichertl.hopkinsStatistic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractPrimitiveDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.AllOrNoneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualSizeGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * The Hopkins Statistic measures the probability that a dataset is generated by
 * a uniform data distribution. Data Mining Concepts and Techniques S. 484-485
 * 
 * @author Lisa Reichert
 * 
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class HopkinsStatistic<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends AbstractPrimitiveDistanceBasedAlgorithm<V, D, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HopkinsStatistic.class);

  public static final OptionID SAMPLESIZE_ID = OptionID.getOrCreateOptionID("hopkins.samplesizes", "List of the size of datasamples");

  /**
   * The parameter sampleSizes
   */
  private List<Integer> sampleSizes = new ArrayList<Integer>();
  
  /**
   * Parameter to specify the number of repetitions of computing the hopkins value.
   */
  public static final OptionID REP_ID = OptionID.getOrCreateOptionID("hopkins.rep", "The number of repetitions.");

  private int rep; 
  
  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("hopkins.seed", "The random number generator seed.");

  /**
   * Holds the value of {@link #SEED_ID}.
   */
  private Long seed;

  /**
   * Parameter for minimum.
   */
  public static final OptionID MINIMA_ID = OptionID.getOrCreateOptionID("dimension.min", "a comma separated concatenation of the minimum values in each dimension. If no value is specified, the minimum value of the attribute range in this dimension will be taken. If only one value is specified, this value will be taken for all dimensions.");

  /**
   * Parameter for maximum.
   */
  public static final OptionID MAXIMA_ID = OptionID.getOrCreateOptionID("normalize.max", "a comma separated concatenation of the maximum values in each dimension. If no value is specified, the maximum value of the attribute range in this dimension will be taken.  If only one value is specified, this value will be taken for all dimensions.");

  /**
   * Stores the maximum in each dimension.
   */
  private double[] maxima = new double[0];

  /**
   * Stores the minimum in each dimension.
   */
  private double[] minima = new double[0];

  public HopkinsStatistic(PrimitiveDistanceFunction<? super V, D> distanceFunction, List<Integer> sampleSizes, Long seed, int rep, double[] minima, double[] maxima) {
    super(distanceFunction);
    this.sampleSizes = sampleSizes;
    this.seed = seed;
    this.rep = rep;
    this.minima = minima;
    this.maxima = maxima;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  public HopkinsResult run(Database database, Relation<V> relation) {
    final DistanceQuery<V, D> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final Random masterRandom = (this.seed != null) ? new Random(this.seed) : new Random();
    KNNQuery<V, D> knnQuery = database.getKNNQuery(distanceQuery, 2);
    double hopkins;
    DoubleDoublePair pair;
    ArrayList<DoubleDoublePair> result = new ArrayList<DoubleDoublePair>();

    for(int i = 0; i < sampleSizes.size(); i++) {
      int sampleSize = sampleSizes.get(i);
      double h = 0;
      logger.fine("rep" + rep);
      //compute the hopkins value several times an use the average value for a more stable result
      for(int j=0; j < this.rep; j++){
        h += computeHopkinsValue(knnQuery, relation, masterRandom, sampleSize);
      }
      hopkins = h/this.rep;
      // turn into result object
      pair = new DoubleDoublePair(hopkins, sampleSize);
      result.add(pair);
    }
    return new HopkinsResult(result);
  }

  private double computeHopkinsValue(KNNQuery<V, D> knnQuery, Relation<V> relation, Random masterRandom, int sampleSize) {
    // compute NN distances for random objects from within the database
    ModifiableDBIDs dataSampleIds = DBIDUtil.randomSample(relation.getDBIDs(), sampleSize, masterRandom.nextLong());
    Iterator<DBID> iter2 = dataSampleIds.iterator();
    // k= 2 und dann natürlich 2. element aus liste holen sonst nächster nachbar
    // von q immer q
    double b = knnQuery.getKNNForDBID(iter2.next(), 2).get(1).getDistance().doubleValue();
    while(iter2.hasNext()) {
      b += knnQuery.getKNNForDBID(iter2.next(), 2).get(1).getDistance().doubleValue();
    }

    // compute NN distances for randomly created new uniform objects
    Collection<V> uniformObjs = getUniformObjs(relation, masterRandom, sampleSize);
    Iterator<V> iter = uniformObjs.iterator();
    double a = knnQuery.getKNNForObject(iter.next(), 1).get(0).getDistance().doubleValue();
    while(iter.hasNext()) {
      a += knnQuery.getKNNForObject(iter.next(), 1).get(0).getDistance().doubleValue();
    }

    // compute hopkins statistik
    double result = b / (a + b);
    return result;
  }

  public <T extends V> Collection<V> getUniformObjs(Relation<V> relation, Random masterRandom, int sampleSize) {
    int dim = DatabaseUtil.dimensionality(relation);
    ArrayList<V> result = new ArrayList<V>(sampleSize);
    double[] vec = new double[dim];
    Random[] randoms = new Random[dim];
    for(int i = 0; i < randoms.length; i++) {
      randoms[i] = new Random(masterRandom.nextLong());
    }

    V factory = DatabaseUtil.assumeVectorField(relation).getFactory();
    // if no parameter for min max compute min max values for each dimension
    // from dataset
    if(this.minima == null || this.maxima == null || this.minima.length == 0 || this.maxima.length == 0) {
      Pair<V, V> minmax = DatabaseUtil.computeMinMax(relation);
      this.minima = new double[dim];
      this.maxima = new double[dim];
      for(int d = 0; d < dim; d++) {
        minima[d] = minmax.first.doubleValue(d + 1);
        maxima[d] = minmax.second.doubleValue(d + 1);
      }
    }
    // if only one value for all dimensions set this value for each dimension
    if(this.minima.length == 1 || this.maxima.length == 1) {
      double val = minima[0];
      this.minima = new double[dim];
      for(int i = 0; i < dim; i++) {
        minima[i] = val;
      }
      val = maxima[0];
      this.maxima = new double[dim];
      for(int i = 0; i < dim; i++) {
        maxima[i] = val;
      }
    }
    // compute uniform objects
    for(int i = 0; i < sampleSize; i++) {
      for(int d = 0; d < dim; d++) {
        vec[d] = minima[d] + (randoms[d].nextDouble() * (maxima[d] - minima[d]));
      }
      V newp = factory.newInstance(vec);
      result.add(newp);
    }

    return result;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  public static class HopkinsResult extends CollectionResult<DoubleDoublePair> {

    /**
     * Constructor.
     * 
     * @param hopkins result value Hopkinsstatistic
     */
    public HopkinsResult(Collection<DoubleDoublePair> hopkinsResult) {
      super("Hopkinsstatistic", "hopkins", hopkinsResult);
    }

  }

  /**
   * Parameterization class.
   * 
   * @author Lisa Reichert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<V, D> {

    protected List<Integer> sampleSizes;
    
    protected int rep;
    
    protected Long seed;

    /**
     * Stores the maximum in each dimension.
     */
    private double[] maxima = new double[0];

    /**
     * Stores the minimum in each dimension.
     */
    private double[] minima = new double[0];

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter r = new IntParameter(REP_ID, 1);
      if(config.grab(r)) {
        rep = r.getValue();
      }
      else{
        rep = r.getDefaultValue();
      }
        
      IntListParameter sample = new IntListParameter(SAMPLESIZE_ID);
      if(config.grab(sample)) {
        sampleSizes = sample.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
      DoubleListParameter minimaP = new DoubleListParameter(MINIMA_ID, true);
      if(config.grab(minimaP)) {
        List<Double> min_list = minimaP.getValue();
        minima = Util.unbox(min_list.toArray(new Double[min_list.size()]));
      }
      DoubleListParameter maximaP = new DoubleListParameter(MAXIMA_ID, true);
      if(config.grab(maximaP)) {
        List<Double> max_list = maximaP.getValue();
        maxima = Util.unbox(max_list.toArray(new Double[max_list.size()]));
      }

      ArrayList<Parameter<?, ?>> global_1 = new ArrayList<Parameter<?, ?>>();
      global_1.add(minimaP);
      global_1.add(maximaP);
      config.checkConstraint(new AllOrNoneMustBeSetGlobalConstraint(global_1));

      ArrayList<ListParameter<?>> global = new ArrayList<ListParameter<?>>();
      global.add(minimaP);
      global.add(maximaP);
      config.checkConstraint(new EqualSizeGlobalConstraint(global));

    }

    @Override
    protected HopkinsStatistic<V, D> makeInstance() {
      return new HopkinsStatistic<V, D>(distanceFunction, sampleSizes, seed,rep, minima, maxima);
    }
  }
}
