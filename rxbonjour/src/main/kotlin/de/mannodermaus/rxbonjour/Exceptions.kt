package de.mannodermaus.rxbonjour

class IllegalBonjourTypeException(type: String) : RuntimeException("The following is not a valid Bonjour type: $type")
class DiscoveryFailedException(driverName: String, cause: Exception?) : RuntimeException("Service Discovery failed with an unrecoverable error (driver: $driverName)", cause)
class BroadcastFailedException(driverName: String, cause: Exception?) : RuntimeException("Service Broadcast failed with an unrecoverable error (driver: $driverName)", cause)
