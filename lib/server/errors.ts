import { NextResponse } from "next/server";

export class HttpError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function badRequest(message: string): never {
  throw new HttpError(400, message);
}

export function unauthorized(message: string): never {
  throw new HttpError(401, message);
}

export function notFound(message: string): never {
  throw new HttpError(404, message);
}

export function conflict(message: string): never {
  throw new HttpError(409, message);
}

export function jsonError(error: unknown) {
  if (error instanceof HttpError) {
    return NextResponse.json(
      {
        timestamp: new Date().toISOString(),
        status: error.status,
        error: httpStatusText(error.status),
        message: error.message,
      },
      { status: error.status }
    );
  }

  console.error(error);
  return NextResponse.json(
    {
      timestamp: new Date().toISOString(),
      status: 500,
      error: "Internal Server Error",
      message: "Internal server error",
    },
    { status: 500 }
  );
}

export function isUniqueViolation(error: unknown): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    "code" in error &&
    (error as { code?: string }).code === "23505"
  );
}

function httpStatusText(status: number): string {
  switch (status) {
    case 400:
      return "Bad Request";
    case 401:
      return "Unauthorized";
    case 404:
      return "Not Found";
    case 409:
      return "Conflict";
    default:
      return "Error";
  }
}
