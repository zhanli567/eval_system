import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

interface CompanyResponse<T> {
  resultObjVO?: T
  msg?: string
  message?: string
}

type UnwrappedResponse<T> = T extends ApiResponse<infer Data>
  ? Data
  : T extends CompanyResponse<infer Data>
    ? Data
    : T

export interface HttpResponse<T> {
  data: T
}

interface HttpOptions {
  params?: Record<string, unknown>
}

type NetworkMethod = (url: string, data?: unknown) => Promise<unknown>

export interface NetworkClient {
  get?: NetworkMethod
  post?: NetworkMethod
  put?: NetworkMethod
  delete?: NetworkMethod
}

interface AuroraRuntime {
  service?: {
    network?: NetworkClient
  }
}

let injectedNetwork: NetworkClient | null = null

const axiosHttp = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export function setNetworkClient(network: NetworkClient | null) {
  injectedNetwork = network
}

export const http = {
  get<T>(url: string, options?: HttpOptions) {
    const network = activeNetwork()
    if (network) {
      return callNetwork<T>(network, 'get', withQuery(url, options?.params))
    }
    return axiosHttp.get<T>(url, options).then((response) => toHttpResponse<T>(response))
  },
  post<T>(url: string, data?: unknown, options?: HttpOptions) {
    const network = activeNetwork()
    if (network) {
      return callNetwork<T>(network, 'post', withQuery(url, options?.params), data)
    }
    return axiosHttp.post<T>(url, data, options).then((response) => toHttpResponse<T>(response))
  },
  put<T>(url: string, data?: unknown, options?: HttpOptions) {
    const network = activeNetwork()
    if (network) {
      return callNetwork<T>(network, 'put', withQuery(url, options?.params), data)
    }
    return axiosHttp.put<T>(url, data, options).then((response) => toHttpResponse<T>(response))
  },
  delete<T>(url: string, options?: HttpOptions) {
    const network = activeNetwork()
    if (network) {
      return callNetwork<T>(network, 'delete', withQuery(url, options?.params))
    }
    return axiosHttp.delete<T>(url, options).then((response) => toHttpResponse<T>(response))
  }
}

export async function unwrap<T>(request: Promise<HttpResponse<T>>): Promise<UnwrappedResponse<T>> {
  try {
    const payload = (await request).data
    if (isApiResponse<T>(payload)) {
      if (payload.code !== 0) {
        throw new Error(payload.msg || 'Request failed')
      }
      return payload.data as UnwrappedResponse<T>
    }
    if (isRecord(payload) && 'resultObjVO' in payload) {
      return payload.resultObjVO as UnwrappedResponse<T>
    }
    return payload as UnwrappedResponse<T>
  } catch (error) {
    throw new Error(errorMessage(error, 'Request failed'))
  }
}

export function errorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (isRecord(error)) {
    const response = error.response
    const data = isRecord(response) ? response.data : undefined
    if (isRecord(data)) {
      return stringValue(data.msg) || stringValue(data.message) || fallback
    }
  }
  return fallback
}

function activeNetwork() {
  return injectedNetwork ?? globalNetwork()
}

function globalNetwork() {
  const globalScope = globalThis as typeof globalThis & {
    Aurora?: AuroraRuntime
    $service?: { network?: NetworkClient }
  }
  return globalScope.Aurora?.service?.network ?? globalScope.$service?.network ?? null
}

function callNetwork<T>(network: NetworkClient, method: keyof NetworkClient, url: string, data?: unknown) {
  const request = network[method]
  if (!request) {
    return Promise.reject(new Error(`Network client does not support ${method}`))
  }
  return request(url, data).then(toHttpResponse<T>)
}

function toHttpResponse<T>(response: unknown): HttpResponse<T> {
  if (isRecord(response) && 'data' in response) {
    return { data: response.data as T }
  }
  return { data: response as T }
}

function withQuery(url: string, params?: Record<string, unknown>) {
  if (!params) return url
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue
    if (Array.isArray(value)) {
      value.forEach((item) => query.append(key, String(item)))
    } else {
      query.append(key, String(value))
    }
  }
  const queryString = query.toString()
  return queryString ? `${url}${url.includes('?') ? '&' : '?'}${queryString}` : url
}

function isApiResponse<T>(value: unknown): value is ApiResponse<T> {
  return isRecord(value) && typeof value.code === 'number' && 'data' in value
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function stringValue(value: unknown) {
  return typeof value === 'string' && value ? value : ''
}
