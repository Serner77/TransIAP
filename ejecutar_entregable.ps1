# ejecutar_proyecto.ps1 - Script para ejecutar TransIAP

# Ruta base
$base = "C:\Users\Administrador.WIN-2O4P6U7CI32\AnypointStudio\workspace\transiap"

# Función para ejecutar un componente
function EjecutarComponente {
    param($nombre, $clase, $args)
    
    $ruta = "$base\$nombre"
    $classpath = "$ruta\bin"
    
    # Agregar JARs de lib
    $libPath = "$ruta\lib"
    if (Test-Path $libPath) {
        $jars = Get-ChildItem -Path $libPath -Filter "*.jar" -Recurse
        foreach ($jar in $jars) {
            $classpath += ";" + $jar.FullName
        }
    }
    
    # Comando completo
    $comando = "java -cp `"$classpath`" $clase $args"
    
    # Crear script para la ventana
    $scriptContent = @"
cd `"$ruta`"
$comando
pause
"@
    
    # Guardar script temporal
    $tempFile = [System.IO.Path]::GetTempFileName() + ".ps1"
    $scriptContent | Out-File -FilePath $tempFile -Encoding UTF8
    
    # Abrir nueva ventana
    Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "`"$tempFile`""
    
    Start-Sleep -Seconds 2
}

# Mostrar mensaje
Write-Host "========================================"
Write-Host "  INICIANDO PROYECTO TRANS IAP"
Write-Host "========================================"
Write-Host ""

# Verificar Java
try {
    java -version 2>&1 | Out-Null
    Write-Host "Java: OK"
} catch {
    Write-Host "ERROR: Java no encontrado"
    Read-Host "Presiona Enter para salir"
    exit
}

Write-Host ""

# Ejecutar componentes en orden
Write-Host "1. Iniciando Middleware..."
EjecutarComponente "middleware_live" "middleware_live.SoporteLogisticaLive localhost LIVE1"
Start-Sleep -Seconds 3

Write-Host "2. Iniciando Registro BD..."
EjecutarComponente "registro" "registro.RegistroBD localhost REG1"
Start-Sleep -Seconds 3

Write-Host "3. Iniciando Visualizador..."
EjecutarComponente "visualizador" "visualizador.Visualizador localhost VIS1"
Start-Sleep -Seconds 2

Write-Host "4. Iniciando Generador CSV..."
EjecutarComponente "generador_csv" "productor.productor_csv localhost P_CSV"
Start-Sleep -Seconds 1

Write-Host "5. Iniciando Generador GeoJSON..."
EjecutarComponente "generador_geojson" "productor.productor_geojson localhost P_GEOJSON"
Start-Sleep -Seconds 1

Write-Host "6. Iniciando Generador KML..."
EjecutarComponente "generador_kml" "productor.productor_kml localhost P_KML"

Write-Host ""
Write-Host "========================================"
Write-Host "  PROYECTO INICIADO"
Write-Host "========================================"
Write-Host ""
Write-Host "Instrucciones:"
Write-Host "  1. Ve a cualquier generador"
Write-Host "  2. Escribe: matricula latitud longitud"
Write-Host "  3. Presiona Enter"
Write-Host "Para detener: Cierra las ventanas"
Write-Host ""
Read-Host "Presiona Enter para salir"