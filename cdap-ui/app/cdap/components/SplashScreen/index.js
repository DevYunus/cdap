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
      showTitle: true,
      first: '',
      last: '',
      email: ''
    };
    this.doNotShowCheck;
    this.toggleVideo = this.toggleVideo.bind(this);
    this.toggleRegistration = this.toggleRegistration.bind(this);
    this.toggleCheckbox = this.toggleCheckbox.bind(this);
    this.firstOnChange = this.firstOnChange.bind(this);
    this.lastOnChange = this.lastOnChange.bind(this);
    this.emailOnChange = this.emailOnChange.bind(this);
  }
  componentDidMount() {
    MyUserStoreApi
      .get()
      .subscribe(res => {
        this.setState({
          showSplashScreen: (typeof res.property['standalone-welcome-message'] === 'undefined' ? true : res.property['standalone-welcome-message'])
        });
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
  toggleRegistration(){
    this.setState({
      registrationOpen : !this.state.registrationOpen
    });
  }
  onClose() {
    // if(this.doNotShowCheck){
    //  This means we should no longer display the splash screen
    // }
    this.setState({
      showSplashScreen: false
    });
    this.resetWelcomeMessage();
  }
  toggleVideo(){
    this.setState({
      videoOpen : !this.state.videoOpen
    });
  }
  closeVideo(){
    if(this.state.videoOpen){
      this.setState({
        videoOpen: false
      });
    }
  }
  toggleCheckbox() {
    this.doNotShowCheck = !this.doNotShowCheck;
  }
  firstOnChange(e) {
    this.setState({first : e.target.value});
  }
  lastOnChange(e) {
    this.setState({last : e.target.value});
  }
  emailOnChange(e) {
    this.setState({email : e.target.value});
  }
  render() {
    return (
      <div className={!this.state.showSplashScreen ? 'hide' : ''}>
        <div className="splash-screen-backdrop"></div>
        <div className="splash-screen">
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
                <a className="spash-screen-btn" href="http://docs.cask.co/cdap">
                  <div className="btn btn-default">
                    <span className="fa fa-book btn-icon"></span>{T.translate('features.SplashScreen.buttons.getStarted')}
                  </div>
                </a>
                <div
                  className={this.state.showRegistration ? 'btn btn-default spash-screen-btn' : 'hide'}
                  onClick={this.toggleVideo}
                >
                  <span className="fa fa-youtube-play btn-icon"></span>{T.translate('features.SplashScreen.buttons.introduction')}
                </div>
                <div
                  className={this.state.showRegistration ? 'btn btn-default spash-screen-btn' : 'hide'}
                  onClick={this.toggleRegistration}
                >
                  <span className="fa fa-pencil-square btn-icon"></span>{"Registration"}
                </div>
              </div>
              {
                this.state.showRegistration && this.state.registrationOpen ?
                <div>
                  <div className="registration-form">
                    <div>
                        I
                        <input onChange={this.firstOnChange} autoFocus className="first-name" type="text" name="first" id="first" placeholder="First Name" />
                        <input onChange={this.lastOnChange} className="last-name" type="text" name="last" id="last" placeholder="Last Name" />
                        would like to receive product updates and
                        <div className="second-line-form">
                          newsletters from Cask at this email address
                          <input onChange={this.emailOnChange} className="email" type="email" name="email" id="email" placeholder="email@example.com" />
                        </div>
                    </div>
                  </div>
                </div>
                :
                null
              }
              <div className="splash-checkbox">
                <input checked={this.doNotShowCheck} onChange={this.toggleCheckbox} type="checkbox" />
                <span className="splash-checkbox-label"> Don&rsquo;t show this again </span>
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
