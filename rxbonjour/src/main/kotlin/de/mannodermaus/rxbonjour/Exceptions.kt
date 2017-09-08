package de.mannodermaus.rxbonjour

class IllegalBonjourTypeException(type: String) : RuntimeException("The following is not a valid Bonjour type: $type")
class DiscoveryFailedException(driverName: String, code: Int): RuntimeException("Service Discovery failed with an unrecoverable error (driver: $driverName, code: $code)")