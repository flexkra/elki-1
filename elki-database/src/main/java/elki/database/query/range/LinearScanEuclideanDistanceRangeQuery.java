/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.database.query.range;

import elki.data.NumberVector;
import elki.database.ids.*;
import elki.database.query.LinearScanQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;

import net.jafama.FastMath;

/**
 * Optimized linear scan for Euclidean distance range queries.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - SquaredEuclideanDistance
 * 
 * @param <O> Database object type
 */
public class LinearScanEuclideanDistanceRangeQuery<O extends NumberVector> implements RangeQuery<O>, LinearScanQuery {
  /**
   * Relation to scan.
   */
  private Relation<? extends O> relation;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanEuclideanDistanceRangeQuery(DistanceQuery<O> distanceQuery) {
    super();
    this.relation = distanceQuery.getRelation();
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    return getRangeForObject(relation.get(id), range, result);
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
    final Relation<? extends O> relation = this.relation;
    final SquaredEuclideanDistance squared = SquaredEuclideanDistance.STATIC;
    float frange = Math.nextUp((float) range);
    final double sqrange = frange * frange;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double sqdistance = squared.distance(obj, relation.get(iter));
      if(sqdistance <= sqrange) {
        result.add(FastMath.sqrt(sqdistance), iter);
      }
    }
    return result;
  }
}
