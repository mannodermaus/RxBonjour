package de.mannodermaus.rxbonjour.drivers.nsdmanager

class NsdDiscoveryException(code: Int): RuntimeException("NsdManager Discovery error (code=$code)")
class NsdBroadcastException(code: Int): RuntimeException("NsdManager Broadcast error (code=$code)")
