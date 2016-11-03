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

import React, { Component, PropTypes } from 'react';
import 'whatwg-fetch';
import CaskVideo from 'components/CaskVideo';
require('./SplashScreen.less');

import Card from '../Card';
import MyUserStoreApi from '../../api/userstore';
import T from 'i18n-react';

 class SplashScreen extends Component {
  constructor(props) {
    super(props);
    this.props = props;
    this.state = {
      error: '',
      showRegistration: window.CDAP_CONFIG.cdap.standaloneWebsiteSDKDownload,
      showSplashScreen: true,
      registrationOpen: false,
      videoOpen: false,
      showTitle: true
    };
    this.openVideo = this.openVideo.bind(this);
    this.closeVideo = this.closeVideo.bind(this);
  }
  componentDidMount() {
    MyUserStoreApi
      .get()
      .subscribe(res => {
        console.log('response ', res);
        // this.setState({
        //   showSplashScreen: (typeof res.property['standalone-welcome-message'] === 'undefined' ? true : res.property['standalone-welcome-message'])
        // });
      });
  }
  resetWelcomeMessage() {
    MyUserStoreApi
      .get()
      .flatMap(res => {
        res.property['standalone-welcome-message'] = false;
        return MyUserStoreApi.set({}, res.property);
      })
      .subscribe(
        () => {},
        (err) => { this.setState({error: err}); }
      );
  }
  onClose() {
    this.setState({
      showSplashScreen: false
    });
    this.resetWelcomeMessage();
  }
  openVideo(){
    this.setState({
      videoOpen : true
    });
  }
  closeVideo(){
    if(this.state.videoOpen){
      this.setState({
        videoOpen: false
      });
    }
  }
  render() {
    return (
      <div className={!this.state.showSplashScreen ? 'hide' : ''}>
        <div className="splash-screen-backdrop"></div>
        <div className="splash-screen"
          onClick={this.closeVideo}
        >
          <Card
            className="splash-screen-card"
            closeable
            title={T.translate('features.SplashScreen.title')}
            onClose={this.onClose.bind(this)}
            showTitle={!this.state.videoOpen}
          >
            <div className="text-center">
            {
              this.state.videoOpen ?
              <div className="splash-video-container">
                <div className="cask-video-container">
                  <CaskVideo />
                </div>
              </div>
              :
              <div className="splash-main-container">
                <span className="fa fa-5x icon-fist"></span>
                <div className="version-label">
                  {T.translate('features.SplashScreen.version-label')}
                </div>
                <h4>
                  {T.translate('features.SplashScreen.intro-message')}
                </h4>
              </div>
            }

              <br />
              <div className={this.state.showRegistration ? 'group' : 'group no-registration'}>
                <a href="http://docs.cask.co/cdap">
                  <div className="btn btn-default">
                    <span className="fa fa-book btn-icon"></span>{T.translate('features.SplashScreen.buttons.getStarted')}
                  </div>
                </a>
                <div className="btn-buffer">
                </div>
                <div
                  className={this.state.showRegistration ? 'btn btn-default' : 'hide'}
                  onClick={this.openVideo}
                >
                  <span className="fa fa-youtube-play btn-icon"></span>{T.translate('features.SplashScreen.buttons.introduction')}
                </div>
                <div
                  className={this.state.showRegistration ? 'btn btn-default' : 'hide'}
                  onClick={this.openVideo}
                >
                  <span className="fa fa-pencil-square btn-icon"></span>{"Registration"}
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    );
  }
}

const propTypes = {
  openVideo: PropTypes.func
};

SplashScreen.propTypes = propTypes;
export default SplashScreen;
