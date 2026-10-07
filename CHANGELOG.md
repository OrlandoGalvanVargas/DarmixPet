# Registro de cambios

Todos los cambios notables de **DarmixPet** se documentan en este archivo.

El formato sigue el estándar [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

> **Formato de versiones**: `MAYOR.MENOR.PARCHE`
>
> - **MAYOR** → cambios incompatibles o rediseños grandes.
> - **MENOR** → nuevas funcionalidades compatibles.
> - **PARCHE** → correcciones de errores compatibles.

---

## [No publicado]

### Planeado

- Recordatorios inteligentes de descanso basados en la hora del día.
- Nuevas animaciones y reacciones para los tres personajes.
- Estadísticas semanales de tiempo de pantalla.
- Modo "enfoque profundo" con bloqueo temporal de todas las apps vigiladas.

---

## [1.1.0] — 2026-10-06

### Añadido

- Nuevo ícono de la aplicación: se ve más limpio y reconocible en la pantalla de inicio, con la esencia del sombrero de Archi.

### Cambiado

- Ajustes menores de nitidez y proporción en el ícono del lanzador.

### Notas

- Actualización visual: no cambia la lógica de la app ni afecta los datos existentes. Se puede instalar encima de la versión anterior sin perder nada.

---

## [1.0.0] — 2026-10-06

Primera versión oficial de DarmixPet. 🎉

### Añadido

**Personajes**

- Tres compañeros con personalidad, paleta y estilo visual propios:
  - **Archi el Archimago** — Éter · Magia Arcana.
  - **Sylva el Sabio** — Naturaleza · Vitalidad.
  - **Sir Galahad el Guardián** — Tierra · Disciplina.
- Sprites animados para cada personaje (idle, toques, noche, batería baja, escucha, pensamiento).
- Frases motivacionales únicas por personaje (`CharacterPhrases`).
- Cambio de personaje en caliente desde la pantalla **Mascota** y desde el menú rápido.

**Vigilancia de apps**

- Selección de apps a vigilar desde la pantalla principal con búsqueda y filtros.
- Configuración por app: **tiempo por sesión**, **enfriamiento** y **sesiones al día**.
- Contador de uso en tiempo real y reinicio automático diario.
- Detección instantánea mediante `AccessibilityService` para bloquear apps vigiladas a tiempo.
- Bloqueo con redirección al inicio y overlay animado de pantalla completa.

**Mascota flotante**

- Overlay persistente con `TYPE_APPLICATION_OVERLAY` que vive sobre otras apps.
- Gestos: toque simple, doble toque, triple toque, mantener presionado y arrastrar.
- Burbujas de diálogo tipo sticker con animaciones `popIn` / `popOut`.
- Anclaje inteligente del bocadillo alrededor de la mascota.

**Menú rápido**

- Apertura por pulsación larga sobre la mascota.
- Control de **brillo** del sistema sin salir de la app.
- Control de **volumen** multimedia.
- Acceso a la app de **temporizador** (compatible con `AlarmClock.ACTION_SET_TIMER`).
- Página de estado con contador de tiempo restante de la app en primer plano.
- Cambio de personaje y acceso directo a la configuración.

**Avisos contextuales**

- Alerta persistente de **batería baja** con dibujo animado de la pila.
- Recordatorio **nocturno** cuando es muy tarde.
- Detección de **música en reproducción** para poner a la mascota en modo escucha.
- Aviso de mitad de sesión con voz TTS.

**Interfaz**

- Diseño "sticker" consistente: contornos gruesos, sombras sólidas desplazadas y esquinas muy redondeadas.
- Tres estilos visuales por personaje: `ARCANE_GLOW`, `ORGANIC_LEAF`, `ANGULAR_SHIELD`.
- Tipografía Fredoka (títulos) + Nunito (cuerpo).
- Tema claro, oscuro y automático según el sistema.
- Splash animado tipo "despertar de Archi".
- Pantalla de permisos guiada paso a paso con diálogos explicativos.
- Diálogo de guía interactiva la primera vez que se abre la app.
- Tarjeta "Acerca de" con versión dinámica desde `PackageManager`.

**Persistencia**

- Base de datos Room para apps vigiladas, tiempos y sesiones.
- SharedPreferences para preferencias de tema, skin activa y guía vista.

**Servicios del sistema**

- `ForegroundService` con notificación persistente.
- Reinicio automático del servicio tras cierre por el sistema (`START_STICKY` + `onTaskRemoved`).
- Soporte de reinicio al desbloquear la pantalla.

### Notas de instalación

- Requiere Android 8.0 (API 26) o superior.
- En Android 13+ instalada fuera de Google Play, se necesita habilitar **Ajustes restringidos** antes de conceder otros permisos.

---
