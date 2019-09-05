/*
 * Copyright (C) 2017-2017 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
import React from 'react';
import { render, unmountComponentAtNode } from 'react-dom';
import OO_Config from './components/OO_Config';
import './style.css';

window.registerExtension('overops/overops_config_form', options => {

  const { el } = options;

  render(
          <OO_Config/>, el
  );

  return () => unmountComponentAtNode(el);
});
