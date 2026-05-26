FROM node:22-alpine AS build
WORKDIR /app

# Public Keycloak URL (browser). Example: http://20.x.x.x:8180
ARG KEYCLOAK_PUBLIC_URL=http://localhost:8180

COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN sed -i "s|__KEYCLOAK_PUBLIC_URL__|${KEYCLOAK_PUBLIC_URL}|g" src/environments/environment.prod.ts \
    && npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/sam-shop-frontend/browser /usr/share/nginx/html
EXPOSE 80
