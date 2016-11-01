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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.SerializationFactory;
import org.apache.hadoop.io.serializer.Serializer;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.StringInterner;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * An {@link InputSplit} that tags another InputSplit with extra data for use
 * by {@link DelegatingInputFormat}s.
 */
public class TaggedInputSplit extends InputSplit implements Configurable, Writable {

  private static final Gson GSON = new Gson();
  private static final Type STRING_STRING_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

  private String name;
  private Class<? extends InputSplit> inputSplitClass;

  private InputSplit inputSplit;

  @SuppressWarnings("unchecked")
  private Class<? extends InputFormat<?, ?>> inputFormatClass;

  private String mapperClassName;

  private Configuration conf;
  private Map<String, String> inputConfigs;

  public TaggedInputSplit() {
  }

  /**
   * Creates a new TaggedInputSplit.
   *
   * @param name name of the input to which this InputSplit belongs
   * @param inputSplit The InputSplit to be tagged
   * @param conf The configuration to use
   * @param inputConfigs configurations to use for the InputFormat
   * @param inputFormatClass The InputFormat class to use for this job
   * @param mapperClassName The name of the Mapper class to use for this job
   */
  @SuppressWarnings("unchecked")
  public TaggedInputSplit(String name, InputSplit inputSplit, Configuration conf,
                          Map<String, String> inputConfigs,
                          Class<? extends InputFormat> inputFormatClass,
                          String mapperClassName) {
    this.name = name;
    this.inputSplitClass = inputSplit.getClass();
    this.inputSplit = inputSplit;
    this.conf = conf;
    this.inputConfigs = inputConfigs;
    this.inputFormatClass = (Class<? extends InputFormat<?, ?>>) inputFormatClass;
    this.mapperClassName = mapperClassName;
  }

  /**
   * Retrieves the name of this TaggedInputSplit.
   *
   * @return name of the TaggedInputSplit
   */
  public String getName() {
    return name;
  }

  /**
   * Retrieves the original InputSplit.
   *
   * @return The InputSplit that was tagged
   */
  public InputSplit getInputSplit() {
    return inputSplit;
  }

  /**
   * Retrieves the InputFormat class to use for this split.
   *
   * @return The InputFormat class to use
   */
  @SuppressWarnings("unchecked")
  public Class<? extends InputFormat<?, ?>> getInputFormatClass() {
    return inputFormatClass;
  }

  /**
   * Retrieves the name of the Mapper class to use for this split.
   *
   * @return The name of the Mapper class to use
   */
  @SuppressWarnings("unchecked")
  public String getMapperClassName() {
    return mapperClassName;
  }

  @Override
  public long getLength() throws IOException, InterruptedException {
    return inputSplit.getLength();
  }

  @Override
  public String[] getLocations() throws IOException, InterruptedException {
    return inputSplit.getLocations();
  }

  Map<String, String> getInputConfigs() {
    return inputConfigs;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void readFields(DataInput in) throws IOException {
    name = Text.readString(in);
    inputSplitClass = (Class<? extends InputSplit>) readClass(in);
    inputFormatClass = (Class<? extends InputFormat<?, ?>>) readClass(in);
    mapperClassName = Text.readString(in);
    inputConfigs = GSON.fromJson(Text.readString(in), STRING_STRING_MAP_TYPE);
    inputSplit = ReflectionUtils.newInstance(inputSplitClass, conf);
    SerializationFactory factory = new SerializationFactory(conf);
    Deserializer deserializer = factory.getDeserializer(inputSplitClass);
    deserializer.open((DataInputStream) in);
    inputSplit = (InputSplit) deserializer.deserialize(inputSplit);
  }

  private Class<?> readClass(DataInput in) throws IOException {
    String className = StringInterner.weakIntern(Text.readString(in));
    try {
      return conf.getClassByName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("readObject can't find class", e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void write(DataOutput out) throws IOException {
    Text.writeString(out, name);
    Text.writeString(out, inputSplitClass.getName());
    Text.writeString(out, inputFormatClass.getName());
    Text.writeString(out, mapperClassName);
    Text.writeString(out, GSON.toJson(inputConfigs));
    SerializationFactory factory = new SerializationFactory(conf);
    Serializer serializer = factory.getSerializer(inputSplitClass);
    serializer.open((DataOutputStream) out);
    serializer.serialize(inputSplit);
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  @Override
  public String toString() {
    return inputSplit.toString();
  }

}
