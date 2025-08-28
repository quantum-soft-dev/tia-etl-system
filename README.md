# TIA ETL System

> Enterprise-grade ETL platform for telecommunications data processing for the Government of Liberia

## 🎯 Overview

TIA ETL System is a high-performance data processing platform designed to handle telecommunications data from Orange and MTN operators. The system processes ASN.1 and CSV files, providing the government with tools for tax control and service quality monitoring.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Web Console (React)                  │
├─────────────────────────────────────────────────────────┤
│                   Job Manager Service                   │
├──────────────┬────────────────┬─────────────────────────┤
│ File Scanner │ Parser         │ Monitoring              │
│   Service    │ Orchestrator   │   Service               │
├──────────────┴────────────────┴─────────────────────────┤
│            Redis Queue & Distributed Locks              │
├─────────────────────────────────────────────────────────┤
│     PostgreSQL          │        ClickHouse             │
└─────────────────────────────────────────────────────────┘
```

## 📋 Features

- **Automated File Processing**: Scans directories and processes files based on configurable patterns
- **Plugin Architecture**: Dynamic loading of parsers from JAR files
- **Multi-format Support**: ASN.1 (BER/DER), CSV, and extensible for future formats
- **High Performance**: Processes 1-2K files daily with millions of records
- **Fault Tolerance**: Multi-instance deployment with Redis synchronization
- **Real-time Monitoring**: WebSocket-based dashboard for live updates
- **Enterprise Security**: Keycloak integration for SSO and RBAC

## 🚀 Quick Start

### Prerequisites

- Java 21 LTS
- Docker & Docker Compose
- PostgreSQL 16+
- ClickHouse (latest)
- Redis 7+
- Node.js 20 LTS (for Web Console)

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/quantum-soft-dev/tia-etl-system.git
cd tia-etl-system
```

2. **Start infrastructure services**
```bash
docker-compose -f deployment/docker-compose.yml up -d
```

3. **Build the project**
```bash
./gradlew clean build
```

4. **Run services**
```bash
# Start each service in separate terminals
./gradlew :services:file-scanner:bootRun
./gradlew :services:parser-orchestrator:bootRun
./gradlew :services:job-manager:bootRun
./gradlew :services:monitoring:bootRun
```

5. **Start Web Console**
```bash
cd web-console
npm install
npm run dev
```

Access the application at http://localhost:3000
## 📁 Project Structure

```
tia-etl-system/
├── core/                    # Core libraries and APIs
│   ├── parser-api/          # Parser interfaces
│   ├── common/              # Shared utilities
│   └── domain/              # Domain models
├── services/                # Microservices
│   ├── file-scanner/        # Directory scanning service
│   ├── parser-orchestrator/ # Parser coordination service
│   ├── job-manager/         # Job configuration service
│   └── monitoring/          # Monitoring and alerts
├── parsers/                 # Parser implementations
│   ├── asn1-cdr-parser/     # ASN.1 CDR parser
│   ├── csv-quality-parser/  # CSV quality metrics parser
│   └── plugin-loader/       # JAR loading mechanism
├── web-console/             # React frontend
└── deployment/              # Deployment configurations
```

## 🔧 Configuration

### Job Configuration Example

```kotlin
val job = JobConfiguration(
    name = "Orange Voice CDR",
    sourceDirectory = "/data/orange/voice",
    filePattern = Regex(".*\\.asn1$"),
    scanInterval = ScanInterval.Cron("0 */5 * * * *"), // Every 5 minutes
    parserId = "asn1-cdr-parser",
    afterProcessing = ProcessingAction.MOVE_TO_DONE,
    isActive = true
)
```

### Parser Plugin Development

Implement the `DataParser` interface:

```kotlin
class CustomParser : DataParser {
    override fun getMetadata(): ParserMetadata { ... }
    override fun process(context: ProcessingContext): ProcessingResult { ... }
}
```

Package as JAR and place in `/opt/tia/parsers/` directory.

## 🔐 Security

- **Authentication**: Keycloak SSO with OAuth 2.0
- **Authorization**: Role-based access (Admin, Operator, Viewer)
- **Audit Logging**: All user actions are logged
- **Data Encryption**: TLS for all communications

## 📊 Monitoring

- **Health Checks**: `/actuator/health` endpoints
- **Metrics**: Micrometer with custom processing metrics
- **Alerts**: Real-time notifications via WebSocket
- **Logs**: File-based with rotation, critical logs in PostgreSQL

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Performance tests
./gradlew gatlingRun
```

## 📚 Documentation

- [API Documentation](docs/api.md)
- [Deployment Guide](docs/deployment.md)
- [Parser Development Guide](docs/parser-development.md)
- [Troubleshooting](docs/troubleshooting.md)

## 🤝 Development with Agent OS

This project uses Agent OS for AI-assisted development:

```bash
# Generate new service
claude -p "Use /create-spec to create monitoring-service specification"

# Implement service
claude -p "Use /execute-tasks to implement monitoring-service"
```

See [.agent-os/product/](.agent-os/product/) for project documentation.

## 📈 Performance Metrics

- **File Processing**: >100 files/hour
- **Record Throughput**: >100K records/minute
- **API Response**: <200ms p95
- **System Uptime**: 99.9%

## 🚀 Roadmap

See [.agent-os/product/roadmap.md](.agent-os/product/roadmap.md) for detailed development plan.

## 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📄 License

Proprietary - TIA Company / Quantum Soft Dev

## 👥 Team

Developed by Quantum Soft Dev Team with Agent OS assistance.

---

For support, contact: support@quantum-soft.dev

## Repository

https://github.com/quantum-soft-dev/tia-etl-system
