package co.cask.cdap.app.runtime.spark.preview;

import co.cask.cdap.api.preview.DataTracer;
import co.cask.cdap.app.runtime.spark.SparkRuntimeContext;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A {@link Externalizable} implementation of {@link DataTracer} used in Spark program execution.
  * It has no-op for serialize/deserialize operation, with all operations delegated to the {@link SparkRuntimeContext}
  * of the current execution context.
  */
public class SparkDataTracer implements DataTracer, Externalizable {

  private final SparkRuntimeContext sparkRuntimeContext;
  private final String tracerName;

  /**
   * Constructor. It delegates service discovery to the current {@link SparkRuntimeContext}.
   */
  public SparkDataTracer(SparkRuntimeContext sparkRuntimeContext, String tracerName) {
    this.sparkRuntimeContext = sparkRuntimeContext;
    this.tracerName = tracerName;
  }

  @Override
  public void info(String propertyName, Object propertyValue) {
    sparkRuntimeContext.getDataTracer(tracerName).info(propertyName, propertyValue);
  }

  @Override
  public String getName() {
    return tracerName;
  }

  @Override
  public boolean isEnabled() {
    return sparkRuntimeContext.getDataTracer(tracerName).isEnabled();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    // no-op
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    // no-op
  }
}
