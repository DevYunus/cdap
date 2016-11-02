/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.internal.app.runtime.batch.dataset.input;

import co.cask.cdap.data2.dataset2.lib.partitioned.PartitionedFileSetDataset;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An {@link InputFormat} that delegates behavior of InputFormat to multiple other InputFormats.
 *
 * @see PartitionedFileSetDataset#getInputFormatClassName()
 *
 * @param <K> Type of key
 * @param <V> Type of value
 */
public class PartitionInputFormat<K, V> extends InputFormat<K, V> {

  private static final String ROOT_INPUT_FORMAT = PartitionInputFormat.class.getCanonicalName() + ".rootInputFormat";

  @Override
  public List<InputSplit> getSplits(JobContext context) throws IOException, InterruptedException {
    // make sure that user gets the underlying InputSplit?
    List<InputSplit> actualSplits = getRootInputFormat(context.getConfiguration()).getSplits(context);
    return actualSplits;
//    return Lists.transform(actualSplits, new Function<InputSplit, InputSplit>() {
//      @Nullable
//      @Override
//      public InputSplit apply(@Nullable InputSplit input) {
//        return new PartitionTaggedSplit(input, context.getConfiguration(), )
//        return null;
//      }
//    });
  }

  private InputFormat<K, V> getRootInputFormat(Configuration configuration) {
    @SuppressWarnings("unchecked")
    Class<InputFormat<K, V>> inputFormatClass =
      (Class<InputFormat<K, V>>) configuration.getClassByNameOrNull(ROOT_INPUT_FORMAT);
    Preconditions.checkNotNull(inputFormatClass,
                               "Root InputFormat class not found for " + PartitionInputFormat.class.getSimpleName());
    return ReflectionUtils.newInstance(inputFormatClass, configuration);
  }

  @Override
  public RecordReader<K, V> createRecordReader(InputSplit split,
                                               TaskAttemptContext context) throws IOException, InterruptedException {
    return getRootInputFormat(context.getConfiguration()).createRecordReader(split, context);
  }
}
