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

import React from 'react';
import shortid from 'shortid';
import {mount} from 'enzyme';
import MarketPlaceEntity from 'components/MarketPlaceEntity';

jest.setMock('api/market', require('../../../api/market/__mocks__/market.js'));
jest.mock('api/stream');
jest.mock('api/userstore');
jest.mock('api/pipeline');
jest.mock('api/namespace');

const entity = {
  name: 'SampleEntityName',
  version: '1.0.0',
  label: 'Sample Entity Label',
  author: 'test',
  description: 'This is a test entity',
  org: 'Cask',
  created: Date.now(),
  cdapVersion: '4.0.0'
};
const entityId = shortid.generate();

describe('MarketplaceEntity Unit tests', () => {
  it('Should render', () => {
    const marketPlaceEntity = mount(
      <MarketPlaceEntity
        entity={entity}
        entityId={entityId}
      />
    );
    expect(marketPlaceEntity.find('.market-place-package-card').length).toBe(1);
  });
});
