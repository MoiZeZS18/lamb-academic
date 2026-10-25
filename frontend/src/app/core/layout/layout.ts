import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideDynamicIcon, LucideGraduationCap, LucideLogIn, LucideLogOut, LucideMenu, LucideX } from '@lucide/angular';
import { filter } from 'rxjs';
import { AuthService } from '../auth/auth-service';
import { Rol } from '../auth/roles';
import { AvisoService } from '../services/aviso-service';
import { MENU } from './menu';

const NOMBRE_ROL: Record<string, string> = {
  [Rol.ESTUDIANTE]: 'Estudiante',
  [Rol.COORDINADOR]: 'Coordinación académica',
  [Rol.ADMIN]: 'Administración',
  [Rol.CAJERO]: 'Caja',
};

/**
 * Contenedor de navegacion (S7): encabezado, menu lateral y router-outlet. Inicio es publico; el
 * menu solo aparece con sesion y filtra por rol (S11 3.9) con las mismas reglas del backend.
 */
@Component({
  selector: 'app-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideDynamicIcon,
    LucideGraduationCap,
    LucideLogIn,
    LucideLogOut,
    LucideMenu,
    LucideX,
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout {
  protected readonly auth = inject(AuthService);
  protected readonly avisos = inject(AvisoService);
  protected readonly menuAbierto = signal(false);

  protected readonly menu = computed(() => {
    const roles = this.auth.roles();
    return MENU.map((grupo) => ({
      ...grupo,
      items: grupo.items.filter((item) => !item.roles || item.roles.some((r) => roles.includes(r))),
    })).filter((grupo) => grupo.items.length > 0);
  });

  protected readonly rolPrincipal = computed(() => {
    const roles = this.auth.roles();
    const rol = [Rol.ADMIN, Rol.COORDINADOR, Rol.CAJERO, Rol.ESTUDIANTE].find((r) => roles.includes(r));
    return rol ? NOMBRE_ROL[rol] : '';
  });

  constructor() {
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.menuAbierto.set(false));
  }

  /** En pantallas pequenas, Escape cierra el menu lateral desplegado. */
  @HostListener('document:keydown.escape')
  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
  }

  /** "Saltar al contenido": lleva el foco al area principal sin recargar la ruta. */
  protected irAlContenido(evento: Event): void {
    evento.preventDefault();
    document.getElementById('contenido')?.focus();
  }
}
