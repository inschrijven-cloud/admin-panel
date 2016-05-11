package be.thomastoye.speelsysteem

class ConfigurationException(configKey: String)
  extends IllegalStateException(s"Problem with configuration: config key '$configKey' not set or invalid value") {}
