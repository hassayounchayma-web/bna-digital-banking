param(
    [switch]$Tests,
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $repositoryRoot 'backend'

if ($Tests -and $Build) {
    throw 'Choose -Tests or -Build, not both.'
}

function Import-LocalEnvironment {
    $environmentPath = Join-Path $repositoryRoot '.env'
    if (-not (Test-Path $environmentPath)) {
        return
    }

    $lineNumber = 0
    foreach ($line in Get-Content -Path $environmentPath) {
        $lineNumber++
        $trimmedLine = $line.Trim()
        if (-not $trimmedLine -or $trimmedLine.StartsWith('#')) {
            continue
        }

        if ($trimmedLine -notmatch '^(?<name>[A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?<value>.*)$') {
            throw "Invalid .env entry at line $lineNumber; expected NAME=value."
        }

        $name = $Matches['name']
        $value = $Matches['value'].Trim()
        if ($value.Length -ge 2 -and (
                ($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

Import-LocalEnvironment

function Find-Maven {
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        return $mavenCommand.Source
    }

    $wrapperCache = Join-Path $HOME '.m2\wrapper\dists'
    if (Test-Path $wrapperCache) {
        $cachedMaven = Get-ChildItem -Path $wrapperCache -Filter mvn.cmd -File -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\apache-maven-3\.\d+\.\d+\\bin\\mvn\.cmd$' } |
            Sort-Object {
                $match = [regex]::Match($_.FullName, 'apache-maven-(?<version>3\.\d+\.\d+)')
                if ($match.Success) {
                    [version]$match.Groups['version'].Value
                } else {
                    [version]'0.0'
                }
            } -Descending |
            Select-Object -First 1

        if ($cachedMaven) {
            return $cachedMaven.FullName
        }
    }

    throw 'Maven 3.9+ introuvable. Installez Apache Maven ou ajoutez mvn.cmd au PATH.'
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $javaVersionOutput = (& java -version 2>&1 | Out-String)
    $javaExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

if ($javaExitCode -ne 0 -or $javaVersionOutput -notmatch 'version "(?<major>\d+)') {
    throw 'Java is missing or its version cannot be detected. Java 25 is required.'
}

if ([int]$Matches['major'] -lt 25) {
    throw "Java 25 is required; detected version: $($Matches['major'])."
}

$mavenPath = Find-Maven
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $mavenVersionOutput = (& $mavenPath --version 2>&1 | Out-String)
    $mavenExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

if ($mavenExitCode -ne 0 -or $mavenVersionOutput -notmatch 'Apache Maven (?<version>\d+\.\d+\.\d+)') {
    throw "Unable to verify Maven at: $mavenPath"
}

if ([version]$Matches['version'] -lt [version]'3.9.0') {
    throw "Maven 3.9+ is required; detected version: $($Matches['version'])."
}

if (-not $env:JWT_SECRET) {
    $secretBytes = New-Object byte[] 32
    $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomGenerator.GetBytes($secretBytes)
        $env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
    } finally {
        $randomGenerator.Dispose()
    }
    Write-Host 'A temporary JWT key was generated for this local process.'
}

if (-not $Tests -and -not $Build) {
    if (-not $env:DB_URL) {
        $env:DB_URL = 'jdbc:mysql://localhost:3306/bna_digital_banking?serverTimezone=UTC'
    }
    if (-not $env:DB_USERNAME) {
        $env:DB_USERNAME = 'bna_app'
    }
    if (-not $env:DB_PASSWORD) {
        try {
            $securePassword = Read-Host "MySQL password for $env:DB_USERNAME" -AsSecureString
        } catch {
            throw 'Set DB_PASSWORD in the environment to run the backend without an interactive prompt.'
        }
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try {
            $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        }
        if (-not $env:DB_PASSWORD) {
            throw 'The MySQL password cannot be empty.'
        }
    }
}

Push-Location $backendPath
try {
    if ($Tests) {
        & $mavenPath '-Dnet.bytebuddy.experimental=true' 'test'
    } elseif ($Build) {
        & $mavenPath '-DskipTests' 'package'
    } else {
        & $mavenPath 'spring-boot:run'
    }

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
