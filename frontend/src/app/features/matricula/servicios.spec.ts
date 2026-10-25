import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatriculaService } from './matricula/matricula-service';
import { PeriodoService } from './periodo/periodo-service';
import { SeccionService } from './seccion/seccion-service';

const API = 'http://localhost:8080';

describe('Servicios HTTP de Matricula (contratos reales del backend)', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('PeriodoService: CRUD sobre /api/v1/periodos', () => {
    const servicio = TestBed.inject(PeriodoService);
    const periodo = { codigo: '2027-I', nombre: 'P', fechaInicio: '2027-03-01', fechaFin: '2027-07-15', estado: 'PLANIFICADO' as const };
    servicio.listar().subscribe();
    servicio.crear(periodo).subscribe();
    servicio.actualizar(9, periodo).subscribe();
    servicio.eliminar(9).subscribe();
    backend.expectOne({ method: 'GET', url: `${API}/api/v1/periodos` }).flush([]);
    const crear = backend.expectOne({ method: 'POST', url: `${API}/api/v1/periodos` });
    expect(crear.request.body).toEqual(periodo);
    crear.flush({});
    backend.expectOne({ method: 'PUT', url: `${API}/api/v1/periodos/9` }).flush({});
    backend.expectOne({ method: 'DELETE', url: `${API}/api/v1/periodos/9` }).flush(null);
  });

  it('SeccionService: los filtros viajan como parametros solo si tienen valor (filtrado en el servidor)', () => {
    const servicio = TestBed.inject(SeccionService);
    servicio.listar({ periodoId: 4, cursoId: null, estado: 'ABIERTA', conVacantes: true }).subscribe();
    const req = backend.expectOne((r) => r.url === `${API}/api/v1/secciones`);
    expect(req.request.params.get('periodoId')).toBe('4');
    expect(req.request.params.get('estado')).toBe('ABIERTA');
    expect(req.request.params.get('conVacantes')).toBe('true');
    expect(req.request.params.has('cursoId')).toBe(false);
  });

  it('SeccionService: actualizar solo envia docente y vacantes; estado y borrado por sus endpoints', () => {
    const servicio = TestBed.inject(SeccionService);
    servicio.actualizar(1004, { docenteId: 5, limiteVacantes: 26 }).subscribe();
    servicio.cambiarEstado(1004, 'CERRADA').subscribe();
    servicio.eliminar(1004).subscribe();
    expect(backend.expectOne((r) => r.method === 'PUT').request.body).toEqual({ docenteId: 5, limiteVacantes: 26 });
    const patch = backend.expectOne((r) => r.method === 'PATCH');
    expect(patch.request.url).toBe(`${API}/api/v1/secciones/1004/estado`);
    expect(patch.request.body).toEqual({ estado: 'CERRADA' });
    expect(backend.expectOne((r) => r.method === 'DELETE').request.url).toBe(`${API}/api/v1/secciones/1004`);
  });

  it('MatriculaService: busqueda paginada con orden y filtros', () => {
    TestBed.inject(MatriculaService).buscar({ periodoId: 4, estado: 'INICIADA' }, 2, 10, 'id', 'asc').subscribe();
    const req = backend.expectOne((r) => r.url === `${API}/api/v1/matriculas`);
    expect(req.request.params.toString()).toBe('page=2&size=10&sortBy=id&direction=asc&periodoId=4&estado=INICIADA');
  });

  it('MatriculaService: registra cabecera y detalles en UNA sola peticion', () => {
    const solicitud = {
      matricula: { estudianteId: 1033, periodoId: 4, fecha: '2026-10-24', estado: 'INICIADA', numeroCuotas: 5 },
      detalles: [{ seccionId: 1001 }, { seccionId: 1002 }],
    };
    TestBed.inject(MatriculaService).registrar(solicitud).subscribe();
    const req = backend.expectOne(`${API}/api/v1/matriculas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(solicitud);
  });

  it('MatriculaService: acciones de coordinacion', () => {
    const servicio = TestBed.inject(MatriculaService);
    servicio.anular(93).subscribe();
    servicio.generarCargo(1, 3).subscribe();
    servicio.registrarResultado(61, 7, 'APROBADO').subscribe();
    expect(backend.expectOne(`${API}/api/v1/matriculas/93/anular`).request.method).toBe('PATCH');
    expect(backend.expectOne((r) => r.url === `${API}/api/v1/matriculas/1/cargo`).request.params.get('numeroCuotas')).toBe('3');
    expect(backend.expectOne(`${API}/api/v1/matriculas/61/detalles/7/resultado`).request.body).toEqual({ resultado: 'APROBADO' });
  });
});
