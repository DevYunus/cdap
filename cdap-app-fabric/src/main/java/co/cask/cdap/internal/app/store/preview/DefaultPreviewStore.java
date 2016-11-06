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
package co.cask.cdap.internal.app.store.preview;

import co.cask.cdap.api.Transactional;
import co.cask.cdap.api.TxRunnable;
import co.cask.cdap.api.data.DatasetContext;
import co.cask.cdap.api.dataset.DatasetAdmin;
import co.cask.cdap.api.dataset.DatasetManagementException;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.app.store.preview.PreviewStore;
import co.cask.cdap.data.dataset.SystemDatasetInstantiator;
import co.cask.cdap.data2.datafabric.dataset.DatasetsUtil;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.data2.dataset2.MultiThreadDatasetCache;
import co.cask.cdap.data2.transaction.Transactions;
import co.cask.cdap.data2.transaction.TxCallable;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.proto.id.DatasetId;
import co.cask.cdap.proto.id.NamespaceId;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.tephra.RetryStrategies;
import org.apache.tephra.TransactionExecutorFactory;
import org.apache.tephra.TransactionFailureException;
import org.apache.tephra.TransactionSystemClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Default implementation of the {@link PreviewStore} that stores data in in-memory table
 */
public class DefaultPreviewStore implements PreviewStore {

  private static final DatasetId PREVIEW_TABLE_ID = NamespaceId.SYSTEM.dataset(PreviewDataset.PREVIEW_TABLE_NAME);

  private final DatasetFramework dsFramework;
  private final Transactional transactional;

  @Inject
  public DefaultPreviewStore(DatasetFramework framework, TransactionSystemClient txClient) {
    this.dsFramework = framework;
    this.transactional = Transactions.createTransactionalWithRetry(
      Transactions.createTransactional(new MultiThreadDatasetCache(
        new SystemDatasetInstantiator(framework), txClient,
        NamespaceId.SYSTEM, ImmutableMap.<String, String>of(), null, null)),
      RetryStrategies.retryOnConflict(20, 100)
    );
  }

  @Override
  public void put(final ApplicationId applicationId, final String tracerName, final String propertyName,
                  final Object value) {
    try {
      transactional.execute(new TxRunnable() {
        @Override
        public void run(DatasetContext context) throws Exception {
          getPreviewDataset(context).put(applicationId, tracerName, propertyName, value);
        }
      });
    } catch (TransactionFailureException e) {
      throw Transactions.propagate(e);
    }
  }

  @Override
  public Map<String, List<String>> get(final ApplicationId applicationId, final String tracerName) {
    try {
      return Transactions.execute(transactional, new TxCallable<Map<String, List<String>>>() {
        @Override
        public Map<String, List<String>> call(DatasetContext context) throws Exception {
          return getPreviewDataset(context).get(applicationId, tracerName);
        }
      });
    } catch (TransactionFailureException e) {
      throw Transactions.propagate(e);
    }
  }

  @Override
  public void remove(final ApplicationId applicationId) {
    try {
      transactional.execute(new TxRunnable() {
        @Override
        public void run(DatasetContext context) throws Exception {
          getPreviewDataset(context).remove(applicationId);
        }
      });
    } catch (TransactionFailureException e) {
      throw Transactions.propagate(e);
    }
  }

  @VisibleForTesting
  void clear() throws IOException, DatasetManagementException {
    truncate(dsFramework.getAdmin(PREVIEW_TABLE_ID, null));
  }

  private PreviewDataset getPreviewDataset (DatasetContext datasetContext)
    throws IOException, DatasetManagementException {
    Table table =  DatasetsUtil.getOrCreateDataset(datasetContext, dsFramework, PREVIEW_TABLE_ID, Table.class.getName(),
                                                   DatasetProperties.EMPTY);
    return new PreviewDataset(table);
  }

  private void truncate(@Nullable DatasetAdmin admin) throws IOException {
    if (admin != null) {
      admin.truncate();
    }
  }
}
