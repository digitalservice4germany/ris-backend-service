FROM node:22.11.0
WORKDIR /usr/src/app
COPY package*.json ./
RUN npm update --location=global npm && npm install
COPY . .
COPY ./node_modules/@digitalservice4germany/angie/fonts ./public/fonts
EXPOSE 3000
CMD ["npm", "run", "dev", "--", "--host"]
