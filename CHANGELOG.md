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

- Traducción de la app al inglés.
- Recordatorios inteligentes de descanso basados en la hora del día.
- Nuevas animaciones y reacciones para los tres personajes.
- Estadísticas semanales de tiempo de pantalla.
- Modo "enfoque profundo" con bloqueo temporal de todas las apps vigiladas.

---

## [1.3.0] — 2026-10-29

**Notificaciones y un nuevo look.** Tu compañero ahora te ayuda con tus notificaciones, y la app se viste con los colores de tu personaje.

### Añadido

**Notificaciones**

- Panel de notificaciones: un toque a la mascota muestra tus notificaciones una por una, con opción de abrirlas en su app, eliminarlas o pasar a la siguiente.
- Soporte para mensajes agrupados (WhatsApp, Telegram), correos largos (Gmail) y listas.
- Aura roja alrededor de la mascota cuando hay notificaciones pendientes. Se pausa con la pantalla apagada o con un panel abierto.
- Nuevo permiso **Leer notificaciones**, con guía paso a paso en la pantalla de permisos.
- Reconexión automática del lector de notificaciones si Android lo detiene.

**Voz**

- Interruptor **Voz de la mascota** en Ajustes: silencia todos los avisos hablados sin perder los visuales.
- **Avisos de mensajes por voz** para las apps que elijas: WhatsApp (activa por defecto), WhatsApp Business, Telegram, Messenger, Instagram, SMS, Signal, Discord, Gmail y Outlook. Solo se muestran las que tienes instaladas.
- Las ráfagas de mensajes se agrupan en un solo aviso ("Tienes 3 mensajes de WhatsApp").
- Opción para decir quién envía el mensaje (apagada por defecto, por privacidad).
- Opción para mostrar el aviso también en el globito de la mascota.
- Los avisos de mensajes respetan el modo silencio y vibración, No molestar, los chats silenciados, las llamadas y la app que tienes abierta.

**Apariencia**

- Paleta propia para cada personaje en toda la app, en modo claro y oscuro, con transición animada al cambiar de compañero.
- Nuevo inicio animado: tu personaje se materializa en el centro y hace su acción propia (círculo mágico de Archi, hojas de Sylva, aterrizaje con escudo de Galahad).
- Nuevo emblema de DarmixPet: un fantasma con sombrero de mago que sonríe y guiña.
- Aviso de nueva versión al inicio de Ajustes y pie de página con la versión y el enlace a la página oficial.

### Cambiado

- Nuevo modo oscuro en tonos grafito, más cómodo de noche.
- Los estados de las apps (disponible, en descanso, bloqueada) conservan colores fijos con insignias de texto, para distinguirse siempre.
- Globos, menú rápido, panel de notificaciones y aviso de batería comparten un mismo sistema de posición: se acomodan alrededor de la mascota, no se salen de la pantalla y se reacomodan al girarla.
- El pico de los globos sigue a la mascota aunque el globo se desplace.
- Los globos de Sir Galahad adoptan su nueva paleta arena y caramelo.
- La guía "Cómo jugar", la pantalla de permisos y los textos de la app usan el nombre y los colores de tu personaje actual.
- El menú rápido y la guía usan el nuevo emblema en lugar del sombrero.
- Ajustes más compacto: la tarjeta "Acerca de" se reemplaza por un pie de página.
- Se quita de la pantalla de permisos el aviso sobre "se muestra sobre otras apps", que ya está en Ajustes → Ayuda.

### Corregido

- Abrir la app desde el menú rápido ya no la reinicia ni crea una copia encima de la que estaba abierta.
- Se elimina el destello oscuro al abrir la app y al terminar el inicio animado cuando el tema de la app no coincide con el del teléfono.
- Las notificaciones de servicios en segundo plano y las persistentes ya no aparecen en el panel.

### Notas

- Se instala encima de la versión anterior sin perder tus apps vigiladas ni tus ajustes.
- Al abrir la app te pedirá el nuevo permiso **Leer notificaciones**.

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
- Sprites animados para cada personaje (reposo, toques, noche, batería baja, escucha, pensamiento).
- Frases motivacionales únicas por personaje.
- Cambio de personaje en caliente desde la pantalla **Mascota** y desde el menú rápido.

**Vigilancia de apps**

- Selección de apps a vigilar desde la pantalla principal, con búsqueda y filtros.
- Configuración por app: **tiempo por sesión**, **enfriamiento** y **sesiones al día**.
- Contador de uso en tiempo real y reinicio automático diario.
- Detección instantánea mediante `AccessibilityService` para bloquear apps vigiladas a tiempo.
- Bloqueo con redirección al inicio y overlay animado de pantalla completa.

**Mascota flotante**

- Overlay persistente con `TYPE_APPLICATION_OVERLAY` que vive sobre otras apps.
- Gestos: toque simple, doble toque, triple toque, mantener presionado y arrastrar.
- Burbujas de diálogo tipo sticker con animaciones de entrada y salida.
- Anclaje inteligente del bocadillo alrededor de la mascota.

**Menú rápido**

- Apertura por pulsación larga sobre la mascota.
- Control de **brillo** del sistema sin salir de la app.
- Control de **volumen** multimedia.
- Acceso a la app de **temporizador** (compatible con `AlarmClock.ACTION_SET_TIMER`).
- Página de estado con el tiempo restante de la app en primer plano.
- Cambio de personaje y acceso directo a la configuración.

**Avisos contextuales**

- Alerta persistente de **batería baja** con dibujo animado de la pila.
- Recordatorio **nocturno** cuando es muy tarde.
- Detección de **música en reproducción** para poner a la mascota en modo escucha.
- Aviso de mitad de sesión con voz.

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
- SharedPreferences para preferencias de tema, personaje activo y guía vista.

**Servicios del sistema**

- `ForegroundService` con notificación persistente.
- Reinicio automático del servicio tras cierre por el sistema (`START_STICKY` + `onTaskRemoved`).
- Soporte de reinicio al desbloquear la pantalla.

### Notas de instalación

- Requiere Android 8.0 (API 26) o superior.
- En Android 13+ instalada fuera de Google Play, se necesita habilitar **Ajustes restringidos** antes de conceder otros permisos.
