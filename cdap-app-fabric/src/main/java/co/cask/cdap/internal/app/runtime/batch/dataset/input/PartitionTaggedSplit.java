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

import co.cask.cdap.api.dataset.lib.PartitionKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * A {@link TaggedInputSplit} that is tagged with extra data for use by {@link MultiInputFormat}s.
 */
public class PartitionTaggedSplit extends TaggedInputSplit {

  private static final Gson GSON = new Gson();

  private final PartitionKey partitionKey;

  /**
   * Creates a new MultiInputTaggedSplit.
   *
   * @param inputSplit The InputSplit to be tagged
   * @param conf The configuration to use
   * @param partitionKey the {@link PartitionKey} that this {@link InputSplit} corresponds to.
   */
  @SuppressWarnings("unchecked")
  public PartitionTaggedSplit(InputSplit inputSplit, Configuration conf,
                              PartitionKey partitionKey) {
    super(inputSplit, conf);
    this.partitionKey = partitionKey;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void readAdditionalFields(DataInput in) throws IOException {
//    inputConfigs = GSON.fromJson(Text.readString(in), STRING_STRING_MAP_TYPE);
//    inputFormatClass = (Class<? extends InputFormat<?, ?>>) readClass(in);
  }

  @Override
  protected void writeAdditionalFields(DataOutput out) throws IOException {
//    Text.writeString(out, GSON.toJson(inputConfigs));
//    Text.writeString(out, inputFormatClass.getName());
  }

  /**
   * Returns the {@link PartitionKey} that this {@link InputSplit} corresponds to.
   */
  public PartitionKey getPartitionKey() {
    return partitionKey;
  }

}
