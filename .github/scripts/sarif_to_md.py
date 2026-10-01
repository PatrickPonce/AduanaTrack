#!/usr/bin/env python3
"""Consolida varios archivos SARIF en un reporte Markdown de vulnerabilidades.

Uso:
    python sarif_to_md.py --out reporte.md --summary-json resumen.json archivo1.sarif archivo2.sarif ...

Clasifica cada hallazgo en Crítica / Alta / Media / Baja usando, en este orden:
  1. properties.security-severity de la regla (escala CVSS 0-10)
  2. el nivel SARIF del resultado (error / warning / note)
Los secretos detectados por Gitleaks se consideran siempre de severidad Alta.
"""
import argparse
import datetime as dt
import json
import os
import sys
from pathlib import Path

ORDEN = ["Crítica", "Alta", "Media", "Baja"]


def severidad_cvss(valor):
    try:
        v = float(valor)
    except (TypeError, ValueError):
        return None
    if v >= 9.0:
        return "Crítica"
    if v >= 7.0:
        return "Alta"
    if v >= 4.0:
        return "Media"
    return "Baja"


def severidad_nivel(nivel):
    return {"error": "Alta", "warning": "Media", "note": "Baja", "none": "Baja"}.get(
        (nivel or "warning").lower(), "Media"
    )


def leer_sarif(ruta):
    try:
        with open(ruta, encoding="utf-8") as fh:
            return json.load(fh)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"[aviso] no se pudo leer {ruta}: {exc}", file=sys.stderr)
        return None


def hallazgos(sarif):
    for run in sarif.get("runs", []):
        herramienta = run.get("tool", {}).get("driver", {}).get("name", "desconocida")
        reglas = {}
        for regla in run.get("tool", {}).get("driver", {}).get("rules", []) or []:
            reglas[regla.get("id")] = regla
        for res in run.get("results", []) or []:
            regla = reglas.get(res.get("ruleId"), {})
            props = regla.get("properties", {}) or {}
            sev = severidad_cvss(props.get("security-severity"))
            if sev is None:
                nivel = res.get("level") or regla.get("defaultConfiguration", {}).get("level")
                sev = severidad_nivel(nivel)
            if herramienta.lower() == "gitleaks" and sev in ("Media", "Baja"):
                sev = "Alta"
            ubic = (res.get("locations") or [{}])[0].get("physicalLocation", {})
            archivo = ubic.get("artifactLocation", {}).get("uri", "-")
            linea = ubic.get("region", {}).get("startLine", "")
            mensaje = (res.get("message", {}).get("text") or "").strip().replace("\n", " ")
            if len(mensaje) > 160:
                mensaje = mensaje[:157] + "..."
            yield {
                "herramienta": herramienta,
                "severidad": sev,
                "regla": res.get("ruleId", "-"),
                "ubicacion": f"{archivo}:{linea}" if linea else archivo,
                "mensaje": mensaje.replace("|", "\\|"),
            }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--summary-json")
    ap.add_argument("sarif", nargs="*")
    args = ap.parse_args()

    herramientas, todos = [], []
    for ruta in args.sarif:
        sarif = leer_sarif(ruta)
        if not sarif:
            continue
        for run in sarif.get("runs", []):
            nombre = run.get("tool", {}).get("driver", {}).get("name", Path(ruta).stem)
            version = run.get("tool", {}).get("driver", {}).get("version") or run.get(
                "tool", {}
            ).get("driver", {}).get("semanticVersion", "")
            if nombre not in [h for h, _ in herramientas]:
                herramientas.append((nombre, version))
        todos.extend(hallazgos(sarif))

    conteo = {h: {s: 0 for s in ORDEN} for h, _ in herramientas}
    for f in todos:
        conteo.setdefault(f["herramienta"], {s: 0 for s in ORDEN})[f["severidad"]] += 1
    total = {s: sum(c[s] for c in conteo.values()) for s in ORDEN}

    repo = os.environ.get("GITHUB_REPOSITORY", "AduanaTrack")
    rama = os.environ.get("GITHUB_REF_NAME", "local")
    sha = (os.environ.get("GITHUB_SHA") or "")[:7]
    run_url = ""
    if os.environ.get("GITHUB_RUN_ID"):
        run_url = f"{os.environ.get('GITHUB_SERVER_URL', 'https://github.com')}/{repo}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
    fecha = dt.datetime.now(dt.timezone(dt.timedelta(hours=-5))).strftime("%d/%m/%Y %H:%M")

    gate_ok = total["Crítica"] == 0 and total["Alta"] == 0
    L = []
    L.append("# Reporte de revisión de vulnerabilidades — AduanaTrack\n")
    L.append(f"- **Repositorio:** `{repo}` · **Rama:** `{rama}`" + (f" · **Commit:** `{sha}`" if sha else ""))
    L.append(f"- **Fecha (hora Perú):** {fecha}")
    if run_url:
        L.append(f"- **Ejecución:** {run_url}")
    L.append(
        f"- **Quality Gate (0 críticas / 0 altas):** {'✅ APROBADO' if gate_ok else '❌ NO APROBADO'}\n"
    )
    L.append("## Resumen por herramienta\n")
    L.append("| Herramienta | Versión | Crítica | Alta | Media | Baja | Total |")
    L.append("|---|---|---:|---:|---:|---:|---:|")
    for nombre, version in herramientas:
        c = conteo.get(nombre, {s: 0 for s in ORDEN})
        L.append(
            f"| {nombre} | {version or '-'} | {c['Crítica']} | {c['Alta']} | {c['Media']} | {c['Baja']} | {sum(c.values())} |"
        )
    L.append(
        f"| **Total** | | **{total['Crítica']}** | **{total['Alta']}** | **{total['Media']}** | **{total['Baja']}** | **{sum(total.values())}** |\n"
    )
    L.append("## Detalle de hallazgos\n")
    if not todos:
        L.append("_Sin hallazgos._\n")
    else:
        L.append("| # | Severidad | Herramienta | Regla | Ubicación | Descripción |")
        L.append("|---:|---|---|---|---|---|")
        orden = sorted(todos, key=lambda f: (ORDEN.index(f["severidad"]), f["herramienta"], f["ubicacion"]))
        for i, f in enumerate(orden, 1):
            L.append(
                f"| {i} | {f['severidad']} | {f['herramienta']} | `{f['regla']}` | `{f['ubicacion']}` | {f['mensaje']} |"
            )
    L.append(
        "\n> Registrar cada hallazgo Crítico/Alto en Jira (`fix/SCRUM-NN`), corregirlo y volver a ejecutar el escaneo para evidenciar la corrección."
    )

    Path(args.out).write_text("\n".join(L) + "\n", encoding="utf-8")
    if args.summary_json:
        Path(args.summary_json).write_text(
            json.dumps({"total": total, "por_herramienta": conteo, "gate_ok": gate_ok}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
    print(f"Reporte generado: {args.out} · {sum(total.values())} hallazgos · gate {'OK' if gate_ok else 'FALLA'}")


if __name__ == "__main__":
    main()
