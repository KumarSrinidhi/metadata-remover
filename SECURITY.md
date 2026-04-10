# Security Policy

## Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of ExifCleaner seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### Please Do Not

- **Do not** open a public GitHub issue for security vulnerabilities
- **Do not** disclose the vulnerability publicly until it has been addressed

### Please Do

1. **Email** security details to: [your-email@example.com]
2. **Include** the following information:
   - Type of vulnerability
   - Full paths of source file(s) related to the vulnerability
   - Location of the affected source code (tag/branch/commit or direct URL)
   - Step-by-step instructions to reproduce the issue
   - Proof-of-concept or exploit code (if possible)
   - Impact of the vulnerability

### What to Expect

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours
- **Updates**: We will send you regular updates about our progress
- **Timeline**: We aim to address critical vulnerabilities within 7 days
- **Credit**: We will credit you in the security advisory (unless you prefer to remain anonymous)

## Security Best Practices

When using ExifCleaner:

1. **Keep Updated**: Always use the latest version
2. **Verify Downloads**: Check file hashes when downloading releases
3. **File Sources**: Only process files from trusted sources
4. **Permissions**: Run with minimal required permissions
5. **Backups**: Always maintain backups of original files

## Known Security Considerations

### File Processing

- **File Size Limit**: Maximum 500MB per file to prevent memory exhaustion
- **Magic Byte Validation**: Files are validated by signature, not just extension
- **Non-Destructive**: Original files are never modified
- **Local Processing**: No network communication or cloud uploads

### Dependencies

We regularly scan dependencies for known vulnerabilities using:
- OWASP Dependency Check
- GitHub Dependabot
- Manual security reviews

### Privacy

ExifCleaner is designed with privacy in mind:
- **Local-First**: All processing happens locally on your machine
- **No Telemetry**: We do not collect any usage data
- **No Network**: The application does not make network requests
- **No Cloud**: Files are never uploaded to any server

## Security Updates

Security updates are released as soon as possible after a vulnerability is confirmed. Updates are announced via:
- GitHub Security Advisories
- Release notes
- README.md

## Vulnerability Disclosure Policy

We follow responsible disclosure practices:

1. **Private Disclosure**: Report received privately
2. **Investigation**: We investigate and develop a fix
3. **Patch Release**: Security patch is released
4. **Public Disclosure**: Vulnerability details are published after patch is available
5. **Credit**: Reporter is credited (if desired)

## Security Scanning

This project uses automated security scanning:

- **OWASP Dependency Check**: Scans for vulnerable dependencies
- **SpotBugs**: Static analysis for security issues
- **GitHub Dependabot**: Automated dependency updates
- **GitHub Actions**: CI/CD security checks

## Contact

For security concerns, please contact:
- **Email**: [your-email@example.com]
- **PGP Key**: [Optional: Link to PGP public key]

---

Last Updated: 2024-04-11
