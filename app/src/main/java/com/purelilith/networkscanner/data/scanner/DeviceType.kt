package com.purelilith.networkscanner.data.scanner

enum class DeviceType(val label: String) {
    PHONE("Телефон"),
    TABLET("Планшет"),
    COMPUTER("Компьютер"),
    PRINTER("Принтер"),
    ROUTER("Роутер"),
    TV("Телевизор / приставка"),
    SPEAKER("Колонка / медиаплеер"),
    SMART_HOME("Умный дом"),
    SERVER("Сервер / мини-компьютер"),
    UNKNOWN("Неизвестно");

    companion object {
        fun fromName(name: String): DeviceType =
            entries.firstOrNull { it.name == name } ?: UNKNOWN
    }
}

data class TypeResult(val type: DeviceType, val isGuess: Boolean)