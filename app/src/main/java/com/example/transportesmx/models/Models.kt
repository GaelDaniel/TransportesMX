package com.example.transportesmx.models

data class UserData(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "", // Gerente, Coordinador, Conductor, Cliente
    val empresaId: String = "",
    val name: String = ""
)

data class UnitData(
    val id: String = "",
    val numeroEconomico: String = "",
    val placas: String = "",
    val empresaId: String = "",
    val lastLat: Double = 0.0,
    val lastLng: Double = 0.0,
    val status: String = "disponible", // disponible, en ruta
    val battery: Int = 100,
    val lastUpdate: String = ""
)

data class RouteData(
    val id: String = "",
    val empresaId: String = "",
    val orderId: String = "", 
    val unitId: String = "",
    val unitEco: String = "",
    val cliente: String = "",
    val origenName: String = "",
    val destinoName: String = "",
    val origenLat: Double = 0.0,
    val origenLng: Double = 0.0,
    val destinoLat: Double = 0.0,
    val destinoLng: Double = 0.0,
    val fecha: String = "",
    val status: String = "pendiente" // pendiente, en curso, completado
)

data class OrderData(
    val id: String = "",
    val clienteUid: String = "",
    val clienteNombre: String = "",
    val descripcion: String = "",
    val puntoRecogida: String = "",
    val puntoDestino: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destinoLat: Double = 0.0,
    val destinoLng: Double = 0.0,
    val status: String = "solicitado", // solicitado, asignado, en curso, entregado
    val fecha: String = ""
)

data class FuelData(
    val id: String = "",
    val empresaId: String = "",
    val unitId: String = "",
    val unitEco: String = "",
    val cantidad: String = "",
    val precio: String = "",
    val total: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val fecha: String = ""
)

data class ActivityLog(
    val id: String = "",
    val usuario: String = "",
    val accion: String = "",
    val fecha: String = "",
    val tipo: String = "info"
)
