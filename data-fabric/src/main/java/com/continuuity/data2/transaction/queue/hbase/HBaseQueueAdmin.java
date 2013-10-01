package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.queue.QueueName;
import com.continuuity.data.DataSetAccessor;
import com.continuuity.data2.util.hbase.HBaseTableUtil;
import com.continuuity.data2.transaction.queue.QueueAdmin;
import com.continuuity.data2.transaction.queue.QueueConstants;
import com.continuuity.data2.transaction.queue.hbase.coprocessor.HBaseQueueRegionObserver;
import com.continuuity.weave.filesystem.Location;
import com.continuuity.weave.filesystem.LocationFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.SortedMap;

/**
 *
 */
@Singleton
public class HBaseQueueAdmin implements QueueAdmin {

  private final HBaseAdmin admin;
  private final CConfiguration cConf;
  private final LocationFactory locationFactory;
  private String tableName;

  @Inject
  public HBaseQueueAdmin(@Named("HBaseOVCTableHandleHConfig") Configuration hConf,
                         @Named("HBaseOVCTableHandleCConfig") CConfiguration cConf,
                         DataSetAccessor dataSetAccessor,
                         LocationFactory locationFactory) throws IOException {
    this.admin = new HBaseAdmin(hConf);
    this.cConf = cConf;
    // todo: we have to do that because queues do not follow dataset semantic fully (yet)
    this.tableName =
      HBaseTableUtil.getHBaseTableName(dataSetAccessor.namespace("queue", DataSetAccessor.Namespace.SYSTEM));
    this.locationFactory = locationFactory;
  }

  /**
   * Returns the column qualifier for the consumer state column. The qualifier is formed by
   * {@code <groupId><instanceId>}.
   * @param groupId Group ID of the consumer
   * @param instanceId Instance ID of the consumer
   * @return A new byte[] which is the column qualifier.
   */
  public static byte[] getConsumerStateColumn(long groupId, int instanceId) {
    byte[] column = new byte[Longs.BYTES + Ints.BYTES];
    Bytes.putLong(column, 0, groupId);
    Bytes.putInt(column, Longs.BYTES, instanceId);
    return column;
  }

  @Override
  public boolean exists(String name) throws Exception {
    // NOTE: as of now, all queues stored in same table
    return admin.tableExists(tableName);
  }

  @Override
  public void create(String name, @SuppressWarnings("unused") Properties props) throws Exception {
    create(name);
  }

  @Override
  public void create(String name) throws Exception {
    // NOTE: as of now, all queues stored in same table
    byte[] tableNameBytes = Bytes.toBytes(tableName);
    Location jarDir = locationFactory.create(cConf.get(QueueConstants.ConfigKeys.QUEUE_TABLE_COPROCESSOR_DIR,
                                                       QueueConstants.DEFAULT_QUEUE_TABLE_COPROCESSOR_DIR));
    int splits = cConf.getInt(QueueConstants.ConfigKeys.QUEUE_TABLE_PRESPLITS,
                              QueueConstants.DEFAULT_QUEUE_TABLE_PRESPLITS);
    HBaseTableUtil.createTableIfNotExists(admin, tableNameBytes, QueueConstants.COLUMN_FAMILY,
                                           QueueConstants.MAX_CREATE_TABLE_WAIT,
                                           splits, HBaseTableUtil.createCoProcessorJar(jarDir, HBaseQueueRegionObserver.class),
                                           HBaseQueueRegionObserver.class.getName());
  }

  @Override
  public void truncate(String name) throws Exception {
    // NOTE: as of now, all queues stored in same table
    byte[] tableNameBytes = Bytes.toBytes(tableName);
    HTableDescriptor tableDescriptor = admin.getTableDescriptor(tableNameBytes);
    admin.disableTable(tableNameBytes);
    admin.deleteTable(tableNameBytes);
    admin.createTable(tableDescriptor);
  }

  @Override
  public void drop(String name) throws Exception {
    // NOTE: as of now, all queues stored in same table
    admin.disableTable(tableName);
    admin.deleteTable(tableName);
  }

  @Override
  public void dropAll() throws Exception {
    // NOTE: as of now, all queues stored in same table
    drop(tableName);
  }

  @Override
  public void configureInstances(QueueName queueName, long groupId, int instances) throws Exception {
    Preconditions.checkArgument(instances > 0, "Number of consumer instances must be > 0.");

    if (!exists(tableName)) {
      create(tableName);
    }

    HTable hTable = new HTable(admin.getConfiguration(), tableName);

    try {
      byte[] rowKey = queueName.toBytes();

      // Get all latest entry row key of all existing instances
      // Consumer state column is named as "<groupId><instanceId>"
      Get get = new Get(rowKey);
      get.addFamily(QueueConstants.COLUMN_FAMILY);
      get.setFilter(new ColumnPrefixFilter(Bytes.toBytes(groupId)));
      List<HBaseConsumerState> consumerStates = HBaseConsumerState.create(hTable.get(get));

      int oldInstances = consumerStates.size();

      // Nothing to do if size doesn't change
      if (oldInstances == instances) {
        return;
      }
      // Compute and applies changes
      hTable.batch(getConfigMutations(groupId, instances, rowKey, consumerStates, new ArrayList<Mutation>()));

    } finally {
      hTable.close();
    }
  }

  @Override
  public void configureGroups(QueueName queueName, Map<Long, Integer> groupInfo) throws Exception {
    Preconditions.checkArgument(!groupInfo.isEmpty(), "Consumer group information must not be empty.");

    if (!exists(tableName)) {
      create(tableName);
    }

    HTable hTable = new HTable(admin.getConfiguration(), tableName);

    try {
      byte[] rowKey = queueName.toBytes();

      // Get the whole row
      Result result = hTable.get(new Get(rowKey));

      // Generate existing groupInfo, also find smallest rowKey from existing group if there is any
      NavigableMap<byte[], byte[]> columns = result.getFamilyMap(QueueConstants.COLUMN_FAMILY);
      if (columns == null) {
        columns = ImmutableSortedMap.of();
      }
      Map<Long, Integer> oldGroupInfo = Maps.newHashMap();
      byte[] smallest = decodeGroupInfo(groupInfo, columns, oldGroupInfo);

      List<Mutation> mutations = Lists.newArrayList();

      // For groups that are removed, simply delete the columns
      Sets.SetView<Long> removedGroups = Sets.difference(oldGroupInfo.keySet(), groupInfo.keySet());
      if (!removedGroups.isEmpty()) {
        Delete delete = new Delete(rowKey);
        for (long removeGroupId : removedGroups) {
          for (int i = 0; i < oldGroupInfo.get(removeGroupId); i++) {
            delete.deleteColumns(QueueConstants.COLUMN_FAMILY,
                                 getConsumerStateColumn(removeGroupId, i));
          }
        }
        mutations.add(delete);
      }

      // For each group that changed (either a new group or number of instances change), update the startRow
      Put put = new Put(rowKey);
      for (Map.Entry<Long, Integer> entry : groupInfo.entrySet()) {
        long groupId = entry.getKey();
        int instances = entry.getValue();
        if (!oldGroupInfo.containsKey(groupId)) {
          // For new group, simply put with smallest rowKey from other group or an empty byte array if none exists.
          for (int i = 0; i < instances; i++) {
            put.add(QueueConstants.COLUMN_FAMILY,
                    getConsumerStateColumn(groupId, i),
                    smallest == null ? Bytes.EMPTY_BYTE_ARRAY : smallest);
          }
        } else if (oldGroupInfo.get(groupId) != instances) {
          // compute the mutations needed using the change instances logic
          SortedMap<byte[], byte[]> columnMap =
            columns.subMap(getConsumerStateColumn(groupId, 0),
                           getConsumerStateColumn(groupId, oldGroupInfo.get(groupId)));

          mutations = getConfigMutations(groupId, instances, rowKey, HBaseConsumerState.create(columnMap), mutations);
        }
      }
      mutations.add(put);

      // Compute and applies changes
      if (!mutations.isEmpty()) {
        hTable.batch(mutations);
      }

    } finally {
      hTable.close();
    }
  }

  private byte[] decodeGroupInfo(Map<Long, Integer> groupInfo,
                                 Map<byte[], byte[]> columns, Map<Long, Integer> oldGroupInfo) {
    byte[] smallest = null;

    for (Map.Entry<byte[], byte[]> entry : columns.entrySet()) {
      // Consumer state column is named as "<groupId><instanceId>"
      long groupId = Bytes.toLong(entry.getKey());

      // Map key is sorted by groupId then instanceId, hence keep putting the instance + 1 will gives the group size.
      oldGroupInfo.put(groupId, Bytes.toInt(entry.getKey(), Longs.BYTES) + 1);

      // Update smallest if the group still exists from the new groups.
      if (groupInfo.containsKey(groupId)
          && (smallest == null || Bytes.BYTES_COMPARATOR.compare(entry.getValue(), smallest) < 0)) {
        smallest = entry.getValue();
      }
    }
    return smallest;
  }

  private List<Mutation> getConfigMutations(long groupId, int instances, byte[] rowKey,
                                            List<HBaseConsumerState> consumerStates, List<Mutation> mutations) {
    // Find smallest startRow among existing instances
    byte[] smallest = null;
    for (HBaseConsumerState consumerState : consumerStates) {
      if (smallest == null || Bytes.BYTES_COMPARATOR.compare(consumerState.getStartRow(), smallest) < 0) {
        smallest = consumerState.getStartRow();
      }
    }
    Preconditions.checkArgument(smallest != null, "No startRow found for consumer group %s", groupId);

    int oldInstances = consumerStates.size();

    // If size increase, simply pick the lowest startRow from existing instances as startRow for the new instance
    if (instances > oldInstances) {
      // Set the startRow of the new instances to the smallest among existing
      Put put = new Put(rowKey);
      for (int i = oldInstances; i < instances; i++) {
        new HBaseConsumerState(smallest, groupId, i).updatePut(put);
      }
      mutations.add(put);

    } else {
      // If size decrease, reset all remaining instances startRow to min(smallest, startRow)
      // The map is sorted by instance ID
      Put put = new Put(rowKey);
      Delete delete = new Delete(rowKey);
      for (HBaseConsumerState consumerState : consumerStates) {
        HBaseConsumerState newState = new HBaseConsumerState(smallest,
                                                             consumerState.getGroupId(),
                                                             consumerState.getInstanceId());
        if (consumerState.getInstanceId() < instances) {
          // Updates to smallest rowKey
          newState.updatePut(put);
        } else {
          // Delete old instances
          newState.delete(delete);
        }
      }
      if (!put.isEmpty()) {
        mutations.add(put);
      }
      mutations.add(delete);
    }

    return mutations;
  }

  public String getTableName() {
    return tableName;
  }
}
