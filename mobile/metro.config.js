// Metro configuration for Expo.
// https://docs.expo.dev/guides/customizing-metro/
const { getDefaultConfig } = require('expo/metro-config');

/** @type {import('expo/metro-config').MetroConfig} */
const config = getDefaultConfig(__dirname);

// Enable Node-style "exports" field resolution. React Navigation 7 and
// react-hook-form ship ESM builds whose entry points are declared via the
// package "exports" map and import sibling ".js" files; without this, Metro's
// legacy resolver fails to resolve them (e.g. "./usePreventRemove.js").
config.resolver.unstable_enablePackageExports = true;

module.exports = config;
