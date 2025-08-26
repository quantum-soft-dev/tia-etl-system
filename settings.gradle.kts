rootProject.name = "tia-etl-system"

// Core modules
include("core:parser-api")
include("core:common")
include("core:domain")

// Service modules
include("services:file-scanner")
include("services:parser-orchestrator")
include("services:job-manager")
include("services:monitoring")

// Parser modules
include("parsers:asn1-cdr-parser")
include("parsers:csv-quality-parser")
include("parsers:plugin-loader")