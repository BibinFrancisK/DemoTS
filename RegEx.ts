function parseFileName(fileName: string) {
  const regex = /^(\d{4})\/Q(\d)\/S(\d)\/([^/_]+)_extract\.csv$/;
  const match = fileName.match(regex);

  if (!match) {
    throw new Error("Invalid file format");
  }

  const [, year, quarter, segment, adminSystem] = match;

  return {
    year: parseInt(year, 10),
    quarter: parseInt(quarter, 10),
    segment: parseInt(segment, 10),
    adminSystem,
  };
}

// Usage Examples:
console.log(parseFileName("2024/Q1/S1/cca_extract.csv"));
// Output: { year: 2024, quarter: 1, segment: 1, adminSystem: 'cca' }

console.log(parseFileName("2024/Q4/S3/alip_extract.csv"));
// Output: { year: 2024, quarter: 4, segment: 3, adminSystem: 'alip' }
