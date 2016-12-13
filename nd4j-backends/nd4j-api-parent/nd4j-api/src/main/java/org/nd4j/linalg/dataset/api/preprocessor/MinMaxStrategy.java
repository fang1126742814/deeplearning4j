package org.nd4j.linalg.dataset.api.preprocessor;

import lombok.Getter;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastAddOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastDivOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastMulOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastSubOp;
import org.nd4j.linalg.dataset.MinMaxStats;
import org.nd4j.linalg.factory.Nd4j;

/**
 * {@link NormalizerStrategy} implementation that will normalize and denormalize data arrays to a given range, based on
 * statistics of the upper and lower bounds of the population
 *
 * @author Ede Meijer
 */
@Getter
class MinMaxStrategy implements NormalizerStrategy<MinMaxStats> {
    private final double minRange;
    private final double maxRange;

    /**
     * @param minRange the target range lower bound
     * @param maxRange the target range upper bound
     */
    public MinMaxStrategy(double minRange, double maxRange) {
        this.minRange = minRange;
        this.maxRange = Math.max(maxRange, minRange + Nd4j.EPS_THRESHOLD);
    }

    /**
     * Normalize a data array
     *
     * @param array the data to normalize
     * @param stats statistics of the data population
     */
    @Override
    public void preProcess(INDArray array, MinMaxStats stats) {
        if (array.rank() <= 2) {
            array.subiRowVector(stats.getLower());
            array.diviRowVector(stats.getRange());
        }
        // if feature Rank is 3 (time series) samplesxfeaturesxtimesteps
        // if feature Rank is 4 (images) samplesxchannelsxrowsxcols
        // both cases operations should be carried out in dimension 1
        else {
            Nd4j.getExecutioner().execAndReturn(new BroadcastSubOp(array, stats.getLower(), array, 1));
            Nd4j.getExecutioner().execAndReturn(new BroadcastDivOp(array, stats.getUpper(), array, 1));
        }

        // Scale by target range
        array.muli(maxRange - minRange);
        // Add target range minimum values
        array.addi(minRange);
    }

    /**
     * Denormalize a data array
     *
     * @param array the data to denormalize
     * @param stats statistics of the data population
     */
    @Override
    public void revert(INDArray array, MinMaxStats stats) {
        // Subtract target range minimum value
        array.subi(minRange);
        // Scale by target range
        array.divi(maxRange - minRange);

        if (array.rank() <= 2) {
            array.muliRowVector(stats.getRange());
            array.addiRowVector(stats.getLower());
        } else {
            Nd4j.getExecutioner().execAndReturn(new BroadcastMulOp(array, stats.getUpper(), array, 1));
            Nd4j.getExecutioner().execAndReturn(new BroadcastAddOp(array, stats.getLower(), array, 1));
        }
    }
}
