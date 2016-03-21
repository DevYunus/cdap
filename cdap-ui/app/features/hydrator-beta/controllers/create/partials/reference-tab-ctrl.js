/*
 * Copyright © 2015 Cask Data, Inc.
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

class ConsoleTabControllerBeta {
  constructor(PluginConfigFactoryBeta, NodeConfigStoreBeta, GLOBALS, myHelpers) {
    this.GLOBALS = GLOBALS;
    this.NodeConfigStoreBeta = NodeConfigStoreBeta;
    this.PluginConfigFactoryBeta = PluginConfigFactoryBeta;
    this.myHelpers = myHelpers;
    this.NodeConfigStoreBeta.registerOnChangeListener(this.setState.bind(this));
    this.setState();
  }
  setState() {
    this.state = this.state || {};
    this.state.node = this.NodeConfigStoreBeta.getState().node;
    if (!this.state.node.plugin) {
      this.state.docReference = this.GLOBALS.en.hydrator.studio.info['DEFAULT-REFERENCE'];
    } else {
      let key = `doc.${this.state.node.plugin.name}-${this.state.node.type}`;
      this.PluginConfigFactoryBeta.fetchDocJson(
        this.myHelpers.objectQuery(this.state.node, 'plugin', 'artifact', 'name'),
        this.myHelpers.objectQuery(this.state.node, 'plugin', 'artifact', 'version'),
        this.myHelpers.objectQuery(this.state.node, 'plugin', 'artifact', 'scope'),
        key
      ).then (
        (res) => {
          if (res[key]) {
            this.state.docReference = res[key];
          } else {
            this.state.docReference = this.GLOBALS.en.hydrator.studio.info['NO-REFERENCE'];
          }
        },
        () => this.state.docReference = this.GLOBALS.en.hydrator.studio.info['NO-REFERENCE']
      );
    }
  }
}

ConsoleTabControllerBeta.$inject = ['PluginConfigFactoryBeta', 'NodeConfigStoreBeta', 'GLOBALS', 'myHelpers'];
angular.module(`${PKG.name}.feature.hydrator-beta`)
  .controller('ReferenceTabControllerBeta', ConsoleTabControllerBeta);