/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.aggregations.metrics.percentiles.hdr;

import org.HdrHistogram.DoubleHistogram;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.AggregationStreams;
import org.elasticsearch.search.aggregations.metrics.percentiles.InternalPercentile;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentile;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
*
*/
public class InternalHDRPercentileRanks extends AbstractInternalHDRPercentiles implements PercentileRanks {

    public final static Type TYPE = new Type(PercentileRanks.TYPE_NAME, "hdr_percentile_ranks");

    public final static AggregationStreams.Stream STREAM = new AggregationStreams.Stream() {
        @Override
        public InternalHDRPercentileRanks readResult(StreamInput in) throws IOException {
            InternalHDRPercentileRanks result = new InternalHDRPercentileRanks();
            result.readFrom(in);
            return result;
        }
    };

    public static void registerStreams() {
        AggregationStreams.registerStream(STREAM, TYPE.stream());
    }

    InternalHDRPercentileRanks() {
    } // for serialization

    public InternalHDRPercentileRanks(String name, double[] cdfValues, DoubleHistogram state, boolean keyed, DocValueFormat formatter,
            List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) {
        super(name, cdfValues, state, keyed, formatter, pipelineAggregators, metaData);
    }

    @Override
    public Iterator<Percentile> iterator() {
        return new Iter(keys, state);
    }

    @Override
    public double percent(double value) {
        return percentileRank(state, value);
    }

    @Override
    public String percentAsString(double value) {
        return valueAsString(String.valueOf(value));
    }

    @Override
    public double value(double key) {
        return percent(key);
    }

    @Override
    protected AbstractInternalHDRPercentiles createReduced(String name, double[] keys, DoubleHistogram merged, boolean keyed,
            List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) {
        return new InternalHDRPercentileRanks(name, keys, merged, keyed, format, pipelineAggregators, metaData);
    }

    @Override
    public Type type() {
        return TYPE;
    }

    static double percentileRank(DoubleHistogram state, double value) {
        if (state.getTotalCount() == 0) {
            return Double.NaN;
        }
        double percentileRank = state.getPercentileAtOrBelowValue(value);
        if (percentileRank < 0) {
            percentileRank = 0;
        } else if (percentileRank > 100) {
            percentileRank = 100;
        }
        return percentileRank;
    }

    public static class Iter implements Iterator<Percentile> {

        private final double[] values;
        private final DoubleHistogram state;
        private int i;

        public Iter(double[] values, DoubleHistogram state) {
            this.values = values;
            this.state = state;
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < values.length;
        }

        @Override
        public Percentile next() {
            final Percentile next = new InternalPercentile(percentileRank(state, values[i]), values[i]);
            ++i;
            return next;
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
