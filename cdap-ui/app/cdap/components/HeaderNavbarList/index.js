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
import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import {Link} from 'react-router';
import shortid from 'shortid';

const mapStateToProps = (state) => {
  return {
    namespace : state.selectedNamespace
  };
};

function HeaderNavbarList({list}){
  return (
    <ul className="navbar-list">
        {
          Array.isArray(list) ?
            list.map(item => {
              return (
                <li
                  key={shortid.generate()}
                  className={item.className}
                >
                  {
                    item.disabled ?
                      item.title
                    :
                      <Link
                        to={item.linkTo}
                        activeClassName="active"
                      >
                        {item.title}
                      </Link>
                    }
                </li>
              );
            })
          :
            null
        }
    </ul>
  );
}

HeaderNavbarList.propTypes = {
  list: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string,
    linkTo: PropTypes.string
  })),
  store: PropTypes.object
};

export default connect(mapStateToProps)(HeaderNavbarList);
